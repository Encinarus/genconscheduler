package com.lightpegasus.objectify;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.Duration;

import java.lang.reflect.Type;

/**
* Translator factory for converting a duration to a long for storing in the GAE datastore.
*/
public class DurationLongValueTranslatorFactory extends ValueTranslatorFactory<Duration, Long> {
  public DurationLongValueTranslatorFactory() {
    super(Duration.class);
  }

  @Override
  protected ValueTranslator<Duration, Long> createSafe(Path path, Property property,
      Type type, CreateContext ctx) {
    return new ValueTranslator<Duration, Long>(path, Long.class) {
      @Override
      protected Duration loadValue(Long value, LoadContext ctx) throws SkipException {
        return Duration.millis(value);
      }

      @Override
      protected Long saveValue(Duration value, SaveContext ctx) throws SkipException {
        return value.getMillis();
      }
    };
  }
}
