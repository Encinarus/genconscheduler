package com.lightpegasus.scheduler.gencon;

import com.lightpegasus.scheduler.gencon.entity.GenconEvent;

import java.io.FileInputStream;

/**
 * Created by alek on 5/13/14.
 */
public class EventConverterMain {
  public static void main(String[] args) throws Exception {
    GenconScheduleParser genconEvents = new GenconScheduleParser(
        new FileInputStream("/Users/alek/Downloads/events.xlsx"), 2014);

    for (GenconEvent event : genconEvents) {
      System.out.println("  :: " + event);
    }
  }
}
