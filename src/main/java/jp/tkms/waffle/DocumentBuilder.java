package jp.tkms.waffle;

import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Stream;

public class DocumentBuilder {
  static final MutableDataSet options = new MutableDataSet();

  public static void main(String[] args) {
    options.set(Parser.EXTENSIONS,
      Arrays.asList(
        //AnchorLinkExtension.create(),
        StrikethroughExtension.create(),
        TablesExtension.create(),
        TocExtension.create()
      ));

    Path docsPath = Paths.get("docs");
    walkDirectory(docsPath);
  }

  private static void walkDirectory(Path directory) {
    try (Stream<Path> stream = Files.list(directory)) {
      stream.forEach(path -> {
        if (Files.isDirectory(path)) {
          walkDirectory(path);
        } else if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
          String name = path.getFileName().toString();
          try {
            processMd(path, path.getParent().resolve(name.substring(0, name.length() - 3/* ".md".length() */) + ".html"));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    } catch (IOException e) {
      e.printStackTrace(); // NOP
    }
  }

  private static void processMd(Path mdPath, Path htmlPath) throws IOException {
    System.out.println("# " + mdPath);

    Parser parser = Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).build();

    String markdownText = String.join("\n", Files.readAllLines(mdPath, StandardCharsets.UTF_8));

    Node document = parser.parse(markdownText);
    String htmlText = renderer.render(document);

    Files.writeString(htmlPath, htmlText);
  }
}
