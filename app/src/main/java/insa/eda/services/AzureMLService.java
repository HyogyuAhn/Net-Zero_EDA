package insa.eda.services;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AzureMLService {

    private static final String TAG = "AzureMLService";

    public void sendToAzureModelWithMockData() {
        float accX = 4.8323727f;
        float accY = -0.05358831f;
        float accZ = -0.71993256f;
        float gyroX = 0.03313944f;
        float gyroY = 0.04886922f;
        float gyroZ = -0.23159428f;

        double accMag = Math.sqrt(accX * accX + accY * accY + accZ * accZ);
        double gyroMag = Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);

        double normAccX = (accX - 0.0405) / 0.9855;
        double normAccY = (accY + 0.0734) / 0.9033;
        double normAccZ = (accZ - 0.0083) / 0.9849;
        double normGyroX = (gyroX - 0.0016) / 0.0669;
        double normGyroY = (gyroY + 0.0013) / 0.1262;
        double normGyroZ = (gyroZ - 0.0079) / 0.1157;
        double normAccMag = (accMag - 1.4378) / 0.8349;
        double normGyroMag = (gyroMag - 0.1308) / 0.1294;

        JSONObject jsonInput = new JSONObject();
        JSONObject payload = new JSONObject();
        JSONArray inputArray = new JSONArray();
        JSONObject singleInput = new JSONObject();

        try {
            singleInput.put("AccX", normAccX);
            singleInput.put("AccY", normAccY);
            singleInput.put("AccZ", normAccZ);
            singleInput.put("GyroX", normGyroX);
            singleInput.put("GyroY", normGyroY);
            singleInput.put("GyroZ", normGyroZ);
            singleInput.put("Acc_mag", normAccMag);
            singleInput.put("Gyro_mag", normGyroMag);

            inputArray.put(singleInput);
            payload.put("input1", inputArray);
            jsonInput.put("Inputs", payload);
            jsonInput.put("GlobalParameters", new JSONObject());
        } catch (Exception e) {
            Log.e("AzureJSON", "JSON 생성 실패", e);
            return;
        }

        new Thread(() -> {
            try {
                URL endpoint = new URL("http://9e4599da-99d8-4dea-b48e-5169ea30a6c4.koreacentral.azurecontainer.io/score");
                HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer aBO9HHHRM0FA2VBih6TvSRrRwkxAcPqD");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonInput.toString().getBytes("UTF-8"));
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder responseStr = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line);
                    }
                    
                    JSONObject resultJson = new JSONObject(responseStr.toString());
                    double label = resultJson
                            .getJSONObject("Results")
                            .getJSONArray("WebServiceOutput0")
                            .getJSONObject(0)
                            .getDouble("Scored Labels");

                    Log.d("AzurePrediction", "예측된 라벨: " + label);
                } 

            } catch (Exception e) {
                Log.e("AzureRequest", "요청 실패", e);
            }
        }).start();
    }

    /*
    // 추후 변경 후 구현 예정
    // 실제 사용 시에는 Activity나 Fragment의 onCreate, onViewCreated 등에서 호출해야 합니다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new AzureMLService().sendToAzureModelWithMockData();
    }
    */
}
