package com.blazemeter.jmeter.rte.core.ssh;

public enum SSHCredentialType {
  BY_PROPERTIES("By Properties"),
  BY_FIELDS("By Fields");

  private final String name;

  SSHCredentialType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
