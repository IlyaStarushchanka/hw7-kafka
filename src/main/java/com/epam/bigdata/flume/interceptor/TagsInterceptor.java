package com.epam.bigdata.flume.interceptor;


import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger Logger =
            LoggerFactory.getLogger(TagsInterceptor.class);

    private static final String UserTagsPath = "/tmp/admin/dic/user.profile.tags.us.txt.out";

    //made protected for testing purposes
    private Map<String,String> userTagsDictionary;

    @Override
    public void initialize() {
        Logger.info("Initializing..");
        fillUserTagsDictionary();
    }

    private void fillUserTagsDictionary() {

        List<String> lines = readFile(UserTagsPath);

        userTagsDictionary = lines.stream()
                .skip(1)
                .map(s -> s.split("\\t"))
                .collect(Collectors.toMap(
                        row -> row[0],
                        row -> row[1]
                ));
        Logger.info("{} user tags were loaded", userTagsDictionary.size());
    }

    @Override
    public Event intercept(Event event) {

        Map<String, String> headers = event.getHeaders();
        List<String> messageFields = getMessageFields(event.getBody());

        String userTags = getUserTags(messageFields.get(20));
        String eventDate = messageFields.get(1).substring(0, 8);
        headers.put("event_date", eventDate);
        headers.put("has_user_tags", StringUtils.isNotBlank(userTags) ? "true" : "false" );
        messageFields.add(userTags);

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
        Logger.info("Closing..");
    }


    public List<String> readFile(String path) {
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
