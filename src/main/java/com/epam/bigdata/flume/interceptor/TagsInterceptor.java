package com.epam.bigdata.flume.interceptor;


import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Ilya_Starushchanka on 9/26/2016.
 */
public class TagsInterceptor implements Interceptor {

    private static final String UserTagsPath = "/tmp/admin/dic/user.profile.tags.us.txt.out";

    //made protected for testing purposes
    protected FileReader fileReader = new FileReader();
    private Map<String,String> userTagsDictionary;

    @Override
    public void initialize() {
        fillUserTagsDictionary();
    }

    private void fillUserTagsDictionary() {

        List<String> lines = fileReader.readFile(UserTagsPath);

        userTagsDictionary = lines.stream()
                .skip(1)
                .map(s -> s.split("\\t"))
                .collect(Collectors.toMap(
                        row -> row[0],
                        row -> row[1]
                ));
    }

    @Override
    public Event intercept(Event event) {

        Map<String, String> headers = event.getHeaders();
        List<String> messageFields = getMessageFields(event.getBody());

        String userTagsID = getUserTags(messageFields.get(20));
        headers.put("has_user_tags", StringUtils.isNotBlank(userTagsID) ? "true" : "false" );
        messageFields.add(userTagsID);

        event.setHeaders(headers);
        event.setBody(String.join("\t", messageFields).getBytes());

        return event;
    }

    private List<String> getMessageFields(byte[] messageBody){

        String text = new String(messageBody);
        return new ArrayList<>(Arrays.asList(text.split("\\t")));
    }

    private String getUserTags(String userTagsId) {

        return userTagsDictionary.getOrDefault(userTagsId, "");
    }

    @Override
    public List<Event> intercept(List<Event> events) {

        List<Event> interceptedEvents = new ArrayList<>(events.size());

        for (Event event : events) {
            Event interceptedEvent = intercept(event);
            interceptedEvents.add(interceptedEvent);
        }

        return interceptedEvents;
    }

    @Override
    public void close() {
    }

    static class FileReader
    {

        List<String> readFile(String path) {
            try{

                Configuration hadoopConfig = new Configuration();

                FileSystem fs =  FileSystem.get(new URI("hdfs://sandbox.hortonworks.com"), hadoopConfig);
                Path hdpPath = new Path(path);

                BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(hdpPath)));

                List<String> lines = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {

                    lines.add(line);
                }
                return lines;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Builder
            implements Interceptor.Builder {

        @Override
        public void configure(Context context) {
        }

        @Override
        public Interceptor build() {
            return new TagsInterceptor();
        }
    }
}
