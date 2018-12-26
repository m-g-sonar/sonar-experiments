package org.sonarsource.linguists.data;

import java.util.Date;

public class Commit {

  public enum Role {
    AUTHOR,
    COMMITTER;
  }

  public final Commit.Role role;
  public final String repository;
  public final String user;
  public final Date date;
  public final String sha1;

  public Commit(String repository, String user, Commit.Role role, Date date, String sha1) {
    this.repository = repository;
    this.user = user;
    this.role = role;
    this.date = date;
    this.sha1 = sha1;
  }
}