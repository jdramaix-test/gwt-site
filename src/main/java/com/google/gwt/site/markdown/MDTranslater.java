/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.site.markdown;

import com.google.gwt.site.markdown.fs.MDNode;
import com.google.gwt.site.markdown.fs.MDParent;
import com.google.gwt.site.markdown.toc.TocCreator;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MDTranslater {
  private static final int PEG_DOWN_FLAGS = Extensions.SMARTYPANTS | Extensions.AUTOLINKS |
      Extensions.FENCED_CODE_BLOCKS | Extensions.TABLES | Extensions.DEFINITIONS;

  private PegDownProcessor pegDownProcessor = new PegDownProcessor(PEG_DOWN_FLAGS, Long
      .MAX_VALUE);

  private final TocCreator tocCreator;

  private final MarkupWriter writer;

  private final String template;

  public MDTranslater(TocCreator tocCreator, MarkupWriter writer, String template) {
    this.tocCreator = tocCreator;
    this.writer = writer;
    this.template = template;
  }

  public void render(MDParent root) throws TranslaterException {
    renderTree(root, root);
  }

  private void renderTree(MDNode node, MDParent root) throws TranslaterException {

    if (node.isFolder()) {
      MDParent mdParent = node.asFolder();

      List<MDNode> children = mdParent.getChildren();
      for (MDNode mdNode : children) {
        renderTree(mdNode, root);
      }

    } else {
      String markDown = getNodeContent(node.getPath());
      String htmlMarkDown = pegDownProcessor.markdownToHtml(markDown);

      String toc = tocCreator.createTocForNode(root, node);

      String head = createHeadForNode(node);

      String relativePath = "./";
      for (int i = 1; i < node.getDepth(); i++) {
        relativePath += "../";
      }

      String html = fillTemplate(
          adjustRelativePath(template, relativePath),
          htmlMarkDown,
          adjustRelativePath(toc, relativePath),
          adjustRelativePath(head, relativePath));

      writer.writeHTML(node, html);
    }

  }

  private String createHeadForNode(MDNode node) {
    return "<link href='css/main.css' rel='stylesheet' type='text/css'>";
  }

  private String fillTemplate(String template, String html, String toc, String head) {
    return template.replace("$content", html).replace("$toc", toc).replace("$head", head);
  }

  protected String adjustRelativePath(String html, String relativePath) {
    // Just using Regexp to add relative paths to certain urls.
    // If we wanted to support a more complicated syntax
    // we could parse the template with some library like jsoup
    return html.replaceAll("(href|src)=(['\"])(?:(?:/+)|(?!(?:[a-z]+:|#)))(.*?)(\\2)",
        "$1='" + relativePath + "$3'");
  }

  private String getNodeContent(String path) throws TranslaterException {
    try {
      return Util.getStringFromFile(new File(path));
    } catch (IOException e1) {
      throw new TranslaterException("can not load content from file: '" + path + "'", e1);
    }

  }
}
