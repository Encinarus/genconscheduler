package com.lightpegasus.scheduler.gencon;

import com.lightpegasus.scheduler.gencon.entity.GenconEvent;

import java.io.FileInputStream;

/**
 * Created by alek on 5/13/14.
 */
public class EventConverterMain {
  public static void main(String[] args) throws Exception {
    GenconScheduleParser genconEvents = new GenconScheduleParser(
        new FileInputStream("/Users/alek/projects/genconscheduler/genconscheduler-war/src/main/webapp/WEB-INF/schedules/events.may.9.2014.xlsx"), 2014);

    for (GenconEvent event : genconEvents) {
      System.out.println("  :: " + event);
    }
  }
}
