package com.revolsys.http;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.revolsys.io.map.ObjectFactoryConfig;
import com.revolsys.record.io.format.json.JsonObject;

public class AzureSharedKeyLiteRequestBuilderFactory extends ApacheHttpRequestBuilderFactory {

  public static AzureSharedKeyLiteRequestBuilderFactory forConnectionString(
    final JsonObject connectionParameters) {
    final String accountName = connectionParameters.getString("AccountName");
    final String accountKey = connectionParameters.getString("AccountKey");
    return fromAccountNameAndKey(accountName, accountKey);
  }

  public static AzureSharedKeyLiteRequestBuilderFactory fromAccountNameAndKey(
    final String accountName, final String accountKey) {
    if (accountName != null && accountKey != null) {
      return new AzureSharedKeyLiteRequestBuilderFactory(accountName, accountKey);
    }
    return null;
  }

  public static AzureSharedKeyLiteRequestBuilderFactory fromConfig(
    final ObjectFactoryConfig factoryConfig, final JsonObject config) {
    final String accountName = config.getString("accountName");
    final String secretId = config.getString("secretId");
    final String accountKey = SecretStore.getSecretValue(factoryConfig, secretId, "accountKey");
    return fromAccountNameAndKey(accountName, accountKey);
  }

  private final String accountName;

  private final SecretKeySpec secretKey;

  public AzureSharedKeyLiteRequestBuilderFactory(final String accountName,
    final SecretKeySpec secretKey) {
    this.accountName = accountName;
    this.secretKey = secretKey;
  }

  public AzureSharedKeyLiteRequestBuilderFactory(final String accountName,
    final String accountKey) {
    this.accountName = accountName;
    final byte[] accountKeyBytes = Base64.getDecoder().decode(accountKey);
    this.secretKey = new SecretKeySpec(accountKeyBytes, "HmacSHA256");
  }

  public String getAccountName() {
    return this.accountName;
  }

  public SecretKeySpec getSecretKey() {
    return this.secretKey;
  }

  String getSharedKeyAuthorization(final StringBuilder data)
    throws NoSuchAlgorithmException, InvalidKeyException {
    final String signature = sign(data);
    return String.format("SharedKey %s:%s", this.accountName, signature);
  }

  String getSharedKeyLiteAuthorization(final StringBuilder data)
    throws NoSuchAlgorithmException, InvalidKeyException {
    final String signature = sign(data);
    return String.format("SharedKeyLite %s:%s", this.accountName, signature);
  }

  @Override
  protected ApacheHttpRequestBuilder newRequestBuilder() {
    return new AzureSharedKeyLiteRequestBuilder(this);
  }

  private String sign(final StringBuilder data)
    throws NoSuchAlgorithmException, InvalidKeyException {
    final Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(this.secretKey);
    final byte[] signatureBytes = mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(signatureBytes);
  }

  @Override
  public String toString() {
    return this.accountName.toString();
  }

}
