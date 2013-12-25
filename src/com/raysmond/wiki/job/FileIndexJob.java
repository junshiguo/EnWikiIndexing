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

import com.raysmond.wiki.mr.WeightingIndexMapper;
import com.raysmond.wiki.mr.WeightingIndexReducer;
import com.raysmond.wiki.mr.XmlInputFormat;
import com.raysmond.wiki.util.CounterUtil;
import com.raysmond.wiki.writable.WeightingIndex;

public class FileIndexJob {
	private String inputPath;
	private String outputPath;
	
	public static void main(String[] args) throws Exception {
		FileIndexJob job = new FileIndexJob();

		if (args.length >= 1)
			job.setInputPath(args[0]);

		job.call();
	}
	
	public void call() throws Exception {
		Configuration conf = new Configuration();
		
		// zookeeper
		//conf.set("mapred.job.tracker", "localhost:9001");
		//conf.set("hbase.zookeeper.quorum", "localhost");
		//conf.set("hbase.zookeeper.property.clientPort", "2222");
		
		//input format
		//conf.set(XmlInputFormat.START_TAG_KEY, "<page>");
		//conf.set(XmlInputFormat.END_TAG_KEY, "</page>");
		
		// Job initialization
		Job job = new Job(conf, "File indexing job");
		job.setJarByClass(FileIndexJob.class);
		
		job.setMapperClass(WeightingIndexMapper.class);
		job.setReducerClass(WeightingIndexReducer.class);
		
		//job.setNumReduceTasks(1);
		
		// Map output
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WeightingIndex.class);
		
		
		TableMapReduceUtil.initTableReducerJob("10300240065_31_wikiIndex", WeightingIndexReducer.class, job); 
		
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
		System.out.println("Max word occurence: "+ CounterUtil.getMaxAppearance());
		System.out.println(" => "+ CounterUtil.maxOccurenceWord);
		System.out.println("Max word length: "+ CounterUtil.getMaxWordLength());
		System.out.println(" => "+ CounterUtil.maxLengthWord);
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
