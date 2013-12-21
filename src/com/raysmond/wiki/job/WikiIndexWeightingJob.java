package com.raysmond.wiki.job;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import com.raysmond.wiki.mr.IndexMapper;
import com.raysmond.wiki.mr.IndexReducer;
import com.raysmond.wiki.mr.IndexWeightingMapper;
import com.raysmond.wiki.mr.IndexWeightingReducer;
import com.raysmond.wiki.mr.XmlInputFormat;
import com.raysmond.wiki.util.CounterUtil;
import com.raysmond.wiki.writable.WordIndexWithoutPosition;

public class WikiIndexWeightingJob {
	private String inputPath;
	private String outputPath;
	
	public static void main(String[] args) throws Exception {
		WikiIndexWeightingJob job = new WikiIndexWeightingJob();

		if (args.length >= 1)
			job.setInputPath(args[0]);

		job.call();
	}
	
	public void call() throws Exception {
		//create table
		String tableName = "wikiIndex";
		WikiIndexJob.createHBaseTable(tableName);
		
		//configure mapreduce
		Configuration conf = new Configuration();
		
		// zookeeper
		//conf.set("mapred.job.tracker", "localhost:9001");
		//conf.set("hbase.zookeeper.quorum", "localhost");
		//conf.set("hbase.zookeeper.property.clientPort", "2222");
		
		conf.set(TableOutputFormat.OUTPUT_TABLE, tableName);
		//input format
		//conf.set(XmlInputFormat.START_TAG_KEY, "<page>");
		//conf.set(XmlInputFormat.END_TAG_KEY, "</page>");
		
		// Job initialization
		Job job = new Job(conf, "Wiki Index by HBase");
		job.setJarByClass(WikiIndexWeightingJob.class);
		
		//Mapper and Reducer
		job.setMapperClass(IndexWeightingMapper.class);
		job.setReducerClass(IndexWeightingReducer.class);
		
		//job.setNumReduceTasks(1);
		
		// Map output
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WordIndexWithoutPosition.class);
		
		
		TableMapReduceUtil.initTableReducerJob("wikiIndex", IndexWeightingReducer.class, job); 
		
		// Input and output format
		job.setInputFormatClass(XmlInputFormat.class);
		job.setOutputFormatClass(TableOutputFormat.class);	
		
		// Input path
		FileInputFormat.addInputPath(job, new Path(getInputPath()));
		
		 long startTime = System.currentTimeMillis();
		job.waitForCompletion(true);
		System.out.println("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
		
		System.out.println("Total pages: "+ CounterUtil.getPageCount());
		System.out.println("Total words: "+ CounterUtil.getWordCount());
		System.out.println("Max word occurence pages: "+ CounterUtil.getMaxAppearance());
		System.out.println("Max word length: "+ CounterUtil.getMaxWordLength());
	}

	/**
	 * Create table in HBase
	 * @param tableName
	 * @throws IOException
	 */
	public static void createHBaseTable(String tableName) throws IOException {
		HTableDescriptor htd = new HTableDescriptor(tableName);
		HColumnDescriptor col = new HColumnDescriptor("content");
		htd.addFamily(col);

		Configuration conf = HBaseConfiguration.create();
		//conf.set("hbase.zookeeper.quorum", "localhost");
		//conf.set("hbase.zookeeper.property.clientPort", "2222");
		HBaseAdmin hAdmin = new HBaseAdmin(conf);

		if (hAdmin.tableExists(tableName)) {
			System.out.println("The table already exists, begin to recreate it...");
			hAdmin.disableTable(tableName);
			hAdmin.deleteTable(tableName);
		}

		hAdmin.createTable(htd);
		System.out.println("Table " + tableName + " has been created.");
	}


	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	public String getInputPath() {
		return inputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String getOutputPath() {
		return outputPath;
	}
	
}