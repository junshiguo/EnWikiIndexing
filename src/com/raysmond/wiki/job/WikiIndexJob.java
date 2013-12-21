package com.raysmond.wiki.job;

import java.util.concurrent.Callable;

import javax.xml.soap.Text;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import com.raysmond.wiki.mr.XmlInputFormat;

public class WikiIndexJob implements Callable<String> {

	private String inputPath;
	
	private String outputPath;
	
	private RunningJob runningJob;
	
	@SuppressWarnings("unchecked")
	public String call() throws Exception {
		JobConf job = new JobConf();
		job.setJarByClass(getClass());

		WikiIndexJob.initJobConf(job);

		job.set(XmlInputFormat.START_TAG_KEY, "<page>");
		job.set(XmlInputFormat.END_TAG_KEY, "</page>");

		// Input / Mapper
		FileInputFormat.setInputPaths(job, new Path(getInputPath()));
		job.setInputFormat((Class<? extends InputFormat>) XmlInputFormat.class);

		if (getOutputPath() != null)
			FileOutputFormat.setOutputPath(job, new Path(getOutputPath()));


		JobClient client = new JobClient(job);
		this.runningJob = client.submitJob(job);
		return runningJob.getID().toString();
	}

	public static void initJobConf(JobConf conf) {
		conf.setInputFormat(org.apache.hadoop.mapred.TextInputFormat.class);

		conf.setMapperClass(com.raysmond.wiki.mr.IndexMapper.class);

		conf.setMapOutputKeyClass(org.apache.hadoop.io.Text.class);
		
		//conf.setMapOutputValueClass(org.apache.hadoop.io.Text.class);
		conf.setMapOutputValueClass(com.raysmond.wiki.writable.WordIndex.class);

		conf.setPartitionerClass(org.apache.hadoop.mapred.lib.HashPartitioner.class);
		
		conf.setOutputKeyComparatorClass(org.apache.hadoop.io.Text.Comparator.class);
		
		conf.setReducerClass(com.raysmond.wiki.mr.IndexReducer.class);
		
		// conf.setNumReduceTasks(1);
		
		conf.setOutputKeyClass(Text.class);
		
		conf.setOutputFormat(org.apache.hadoop.mapred.TextOutputFormat.class);
		
		conf.setOutputValueClass(com.raysmond.wiki.writable.IndexList.class);
	}
	
	// getters and setters
	
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

	public RunningJob getRunningJob() {
		return runningJob;
	}

}
