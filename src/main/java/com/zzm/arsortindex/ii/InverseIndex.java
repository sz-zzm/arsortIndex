package com.zzm.arsortindex.ii;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InverseIndex {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf);
		job.setJarByClass(InverseIndex.class);
		job.setMapperClass(IndexMapper.class);
		/**
		 * setMapOutputKeyClass, setMapOutputValueClass在满足一定条件的情况下可以省略
		 * （k2 v2 与 k2 v3 类型一样时可以省略）
		 */
//		job.setMapOutputKeyClass(Text.class);
//		job.setMapOutputValueClass(DataBean.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		job.setCombinerClass(IndexCombiner.class);
		job.setReducerClass(IndexReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.waitForCompletion(true);
	}
	
	public static class IndexMapper extends Mapper<LongWritable, Text, Text, Text> {
		
		private Text k = new Text();
		
		private Text v = new Text();
		
		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] fields = line.split("");
			FileSplit inputSplit = (FileSplit) context.getInputSplit();
			String path = inputSplit.getPath().toString();
			for (String field : fields) {
				k.set(field + "->" + path);
				v.set("1");
				context.write(k, v);
			}
		}
		
	}
	
	public static class IndexCombiner extends Reducer<Text, Text, Text, Text> {
		
		private Text k = new Text();
		
		private Text v = new Text();
		
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String[] wordAndPath = key.toString().split("->");
			String word = wordAndPath[0];
			String path = wordAndPath[1];
			int counter = 0;
			for (Text t : values) {
				counter += Integer.parseInt(t.toString());
			}
			k.set(word);
			v.set(path + "->" +counter);
			context.write(k, v);
		}
		
	}
	
	public static class IndexReducer extends Reducer<Text, Text, Text, Text> {
		
		private Text v = new Text();

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String result = "";
			for (Text t : values) {
				result += t.toString() + "\t";
			}
			v.set(result);
			context.write(key, v);
		}
		
	}
}
