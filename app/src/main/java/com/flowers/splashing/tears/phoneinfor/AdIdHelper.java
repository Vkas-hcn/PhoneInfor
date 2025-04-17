package com.flowers.splashing.tears.phoneinfor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import java.io.IOException;

public class AdIdHelper {
    public static void getAdvertisingId(Context context, final AdIdCallback callback) {
        new Thread(() -> {
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                String advertisingId = adInfo.getId();
                boolean isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();

                // 返回主线程处理结果
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(advertisingId, isLimitAdTrackingEnabled);
                });
            } catch (IOException | GooglePlayServicesNotAvailableException |
                     GooglePlayServicesRepairableException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onFailure("");
                });
            }
        }).start();
    }

    public interface AdIdCallback {
        void onSuccess(String advertisingId, boolean isLimitAdTrackingEnabled);
        void onFailure(String errorMessage);
    }
}
