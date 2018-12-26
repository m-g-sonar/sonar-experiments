package org.sonarsource.linguists;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonarsource.linguists.data.Commit;

public class JsonToCsv {

  private static final File RESULT_FOLDER = new File("target/results");
  private static final File RESULT_FILE = new File("target/results/all.csv");

  private final List<File> files;
  private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(GithubExtractor.DATE_FORMAT);

  public JsonToCsv(File[] files) {
    this.files = Arrays.asList(files);
  }

  public void collectAsCSV() throws IOException {
    List<Commit> fullList = new ArrayList<>();
    for (File jsonFile : files) {
      fullList.addAll(readCommits(jsonFile));
    }
    try (FileOutputStream outputStream = new FileOutputStream(RESULT_FILE)) {
      fullList.stream()
        .map(this::toCSV)
        .forEach(line -> {
          try {
            outputStream.write(line.getBytes());
          } catch (Exception e) {
            System.err.println("Unable to write line");
          }
        });
    } catch (Exception e) {
      System.err.println("===================== Fail to write: " + RESULT_FILE.getName());
    }
  }

  public static void main(String[] args) throws IOException {
    new JsonToCsv(RESULT_FOLDER.listFiles((d, f) -> f.endsWith(".json"))).collectAsCSV();
  }

  public List<Commit> readCommits(File jsonfile) throws IOException {
    String content = Files.readAllLines(jsonfile.toPath()).stream().collect(Collectors.joining("\n"));
    Commit[] commits = GithubExtractor.GSON.fromJson(content, Commit[].class);
    return Arrays.asList(commits);
  }

  public String toCSV(Commit c) {
    return Arrays.asList(
      simpleDateFormat.format(c.date),
      c.user,
      c.repository,
      c.role.name(),
      c.sha1)
      .stream()
      .collect(Collectors.joining(";", "", "\n"));
  }
}
