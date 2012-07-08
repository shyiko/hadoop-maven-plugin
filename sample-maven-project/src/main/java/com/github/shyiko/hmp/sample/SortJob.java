package com.github.shyiko.hmp.sample;

import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class SortJob {

    private static class SortMapper extends Mapper<LongWritable, Text, IntWritable, IntWritable> {

        private IntWritable key = new IntWritable();
        private IntWritable one = new IntWritable(1);

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            this.key.set(Integer.parseInt(value.toString()));
            context.write(this.key, one);
        }
    }

    private static class SortPartitioner extends Partitioner<IntWritable, IntWritable> {

        private int maximumValueInDataSet = 10000;

        @Override
        public int getPartition(IntWritable key, IntWritable value, int numPartitions) {
            return key.get() / (maximumValueInDataSet / numPartitions);
        }
    }

    public static void sort(String input, String output) throws Exception {
        Configuration conf = new Configuration();
        Path inputPath = new Path(input);
        Path outputPath = new Path(output);
        FileSystem fs = outputPath.getFileSystem(conf);
        if (fs.exists(outputPath)) {
            fs.delete(outputPath, true);
        }
        Job job = new Job(conf, "MapReduce Sort");
        job.setNumReduceTasks(2);
        job.setJarByClass(SortJob.class);
        // mapper
        job.setMapperClass(SortMapper.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        // combiner
        job.setCombinerClass(IntSumReducer.class);
        // partitioner
        job.setPartitionerClass(SortPartitioner.class);
        // reducer
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        // input/output
        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);
        job.waitForCompletion(true);
    }


    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(OptionBuilder.isRequired().hasArg().withDescription("input directory").create("i"));
        options.addOption(OptionBuilder.isRequired().hasArg().withDescription("output directory").create("o"));
        try {
            CommandLine commandLine = parser.parse(options, args);
            sort(commandLine.getOptionValue("i"), commandLine.getOptionValue("o"));
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp(SortJob.class.getName(), options);
            System.exit(1);
        }
    }
}
