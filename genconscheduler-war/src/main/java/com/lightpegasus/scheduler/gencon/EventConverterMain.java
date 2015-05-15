package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Function;
import com.google.common.io.ByteStreams;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by alek on 5/13/14.
 */
public class EventConverterMain {
  public static void main(String[] args) throws Exception {
    String uri = "https://www.gencon.com/downloads/events.zip";

    URL url = new URL(uri);
    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
    urlConnection.setRequestMethod("GET");
    urlConnection.connect();
    System.err.println("Content Encoding " + urlConnection.getContentEncoding());
    System.err.println("ResponseCode " + urlConnection.getResponseCode());
    String location = urlConnection.getHeaderField("Location");
    System.err.println("Location " + location);

    url = new URL(location);
    urlConnection = (HttpURLConnection)url.openConnection();
    urlConnection.setRequestMethod("GET");
    urlConnection.connect();
    System.err.println("Content Encoding " + urlConnection.getContentEncoding());
    System.err.println("ResponseCode " + urlConnection.getResponseCode());


    //*
    // FileInputStream input = new FileInputStream("/Users/alek/projects/genconscheduler/genconscheduler-war/src/main/webapp/WEB-INF/schedules/events.may.9.2014.xlsx");
    ZipInputStream zipInputStream = new ZipInputStream(urlConnection.getInputStream());

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    // There should be exactly one entry in the stream
    ZipEntry nextEntry = zipInputStream.getNextEntry();
    while (null != nextEntry) {
      ByteStreams.copy(zipInputStream, out);
      nextEntry = zipInputStream.getNextEntry();
    }
    ByteStreams.copy(new ByteArrayInputStream(out.toByteArray()),
        new FileOutputStream("/Users/alek/Downloads/events2.zip.xlsx"));
    //InputStream input = urlConnection.getInputStream();
    new GenconScheduleParser(
        new FileInputStream("/Users/alek/Downloads/events2.zip.xlsx"), 2015, new Function<GenconEvent, Object>() {
      @Override
      public Object apply(GenconEvent event) {
        System.out.println("  :: " + event);
        return null;
      }
    }).parse();
    // */
  }
}
