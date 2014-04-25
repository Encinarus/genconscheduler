package com.lightpegasus.objectify;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
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
  protected ValueTranslator<Duration, Long> createValueTranslator(
      TypeKey<Duration> tk, CreateContext ctx, Path path) {
    return new ValueTranslator<Duration, Long>(Long.class) {
      @Override
      protected Duration loadValue(Long value, LoadContext ctx, Path path)
          throws SkipException {
        return Duration.millis(value);
      }

      @Override
      protected Long saveValue(Duration value, boolean index, SaveContext ctx, Path path)
          throws SkipException {
        return value.getMillis();
      }
    };
  }
}
