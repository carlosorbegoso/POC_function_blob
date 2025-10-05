package org.sky;

public class KeyAndIV {
  private final byte[] key;
  private final byte[] iv;
  public KeyAndIV(byte[] key, byte[] iv) {
    if(key == null || iv == null){
      throw new IllegalArgumentException("Key and IV must not be null");
    }
    this.key = key;
    this.iv = iv;
  }
  public byte[] getKey() {
    return key.clone();
  }
  public byte[] getIv() {
    return iv.clone();
  }
}
