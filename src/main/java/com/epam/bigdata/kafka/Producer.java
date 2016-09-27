package com.epam.bigdata.kafka;

import com.google.common.io.Resources;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Created by Ilya_Starushchanka on 9/26/2016.
 */
public class Producer {
    public static void main(String[] args) throws IOException {
        // set up the producer
        KafkaProducer<String, String> producer;
        try (InputStream props = Resources.getResource("producer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            producer = new KafkaProducer<>(properties);
        }

        try(Stream<Path> paths = Files.walk(Paths.get(args[0]))) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    try(Stream<String> lines = Files.lines(filePath, Charset.forName("ISO-8859-1"))) {
                        lines.forEach(line ->{
                                producer.send(new ProducerRecord<>("fileLines_new5", line));
                        });
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        } finally {
            producer.close();
        }

    }
}