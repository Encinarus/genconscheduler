package com.lightpegasus.thymeleaf;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 * Controller interface for requests going through the thymeleaf templating engine.
 */
public interface ThymeleafController {
  public void process(WebContext context, TemplateEngine engine) throws Exception;
}
