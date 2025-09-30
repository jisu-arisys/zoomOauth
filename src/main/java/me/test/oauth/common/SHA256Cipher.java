package me.test.oauth.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class SHA256Cipher {

    /** Zoom 웹훅 URL 검증 과정에서 쓰는 HMAC-SHA256 해시 생성 로직 **/
    public static String generateZoomHmac(String secret, String timestamp, String jsonBody) throws Exception {
        String message = "v0:" + timestamp + ":" + jsonBody;
        return "v0=" + generateHmac(secret, message);
    }

    public static String generateHmac(String secret, String plainToken) throws Exception {
        // 1. HMAC-SHA256 알고리즘 초기화
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKey);

        // 2. 토큰 데이터로 해시 생성
        byte[] hashBytes = sha256_HMAC.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

        // 3. 바이트 배열 → HEX 문자열 변환
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

//	public static String SHA256encrypt(String planText) {
//	        try{
//	            MessageDigest md = MessageDigest.getInstance("SHA-256");
//	            md.update(planText.getBytes());
//	            byte byteData[] = md.digest();
//
//	            StringBuffer sb = new StringBuffer();
//	            for (int i = 0; i < byteData.length; i++) {
//	                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
//	            }
//
//	            StringBuffer hexString = new StringBuffer();
//	            for (int i=0;i<byteData.length;i++) {
//	                String hex=Integer.toHexString(0xff & byteData[i]);
//	                if(hex.length()==1){
//	                    hexString.append('0');
//	                }
//	                hexString.append(hex);
//	            }
//
//	            return hexString.toString();
//	        }catch(Exception e){
//	            e.printStackTrace();
//	            throw new RuntimeException();
//	        }
//	    }
//	   public static String SHA512encrypt(String input)
//	    {
//	        try {
//	            // getInstance() method is called with algorithm SHA-512
//	            MessageDigest md = MessageDigest.getInstance("SHA-512");
//
//	            // digest() method is called
//	            // to calculate message digest of the input string
//	            // returned as array of byte
//	            byte[] messageDigest = md.digest(input.getBytes());
//
//	            // Convert byte array into signum representation
//	            BigInteger no = new BigInteger(1, messageDigest);
//
//	            // Convert message digest into hex value
//	            String hashtext = no.toString(16);
//
//	            // Add preceding 0s to make it 32 bit
//	            while (hashtext.length() < 32) {
//	                hashtext = "0" + hashtext;
//	            }
//
//	            // return the HashText
//	            return hashtext;
//	        }
//
//	        // For specifying wrong message digest algorithms
//	        catch (NoSuchAlgorithmException e) {
//	            throw new RuntimeException(e);
//	        }
//	    }
	
}
