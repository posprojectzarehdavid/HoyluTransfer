package com.example.zarehhakobian.hoylu;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Zareh Hakobian on 17.08.2017.
 */

public class ReceiverFragment extends Fragment {

    Socket socket;
    HoyluDevice hoyluDevice;
    ProgressDialog progressDialog;
    ImageView iv;

    public ReceiverFragment() {
    }

    public void initializeDevice() {
        String name = android.os.Build.MODEL;
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        hoyluDevice = new HoyluDevice(id, name, null, id, id, null, null, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeDevice();
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
        try {
            socket = IO.socket(ShareActivity.CONNECTION_STRING);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i("hallo", "connected");
                    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    try {
                        String deviceAsJson = ow.writeValueAsString(hoyluDevice);
                        socket.emit("receiverClient", deviceAsJson);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socket.on("device_registered", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    iv.setImageBitmap(generateQRCode(hoyluDevice.QrValue));
                                }
                            });
                        }
                    });
                    socket.on("getFile", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog = new ProgressDialog(getActivity());
                                    progressDialog.setMessage(getResources().getString(R.string.receiving_data));
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();
                                }
                            });

                            JSONObject jsonObject = (JSONObject) args[0];
                            String originalname = null;
                            String filename = null;
                            try {
                                filename = jsonObject.getString("Filename");
                                originalname = jsonObject.getString("Originalname");
                                downloadFile(filename, originalname);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(socket.connected()== false){
            Toast.makeText(getActivity(), R.string.server_off, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.hoylureceive_view, container, false);
        iv = (ImageView) v.findViewById(R.id.imageView);
        return v;
    }

    private Bitmap generateQRCode(String code) {
        String myCodeText = code;
        int size = 250;

        Map<EncodeHintType, Object> hintMap = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        hintMap.put(EncodeHintType.MARGIN, 1);
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = null;
        try {
            byteMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, size,
                    size, hintMap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        int hoyluWidth = byteMatrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(hoyluWidth, hoyluWidth, Bitmap.Config.RGB_565);

        for (int i = 0; i < hoyluWidth; i++) {
            for (int j = 0; j < hoyluWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    bmp.setPixel(i, j, Color.BLACK);
                } else {
                    bmp.setPixel(i, j, Color.WHITE);
                }
            }
        }
        return bmp;
    }

    public void showAskingDialog(byte[]bytes, final String originalname) {
        progressDialog.dismiss();
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setCancelable(false);
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.selected_image_layout, null);
        ImageView iv = (ImageView) v.findViewById(R.id.selectedImage);
        Bitmap bitmap = null;
        ab.setTitle(R.string.save_or_close);

        if (originalname.endsWith("jpg") || originalname.endsWith("png") ||
                originalname.endsWith("jpeg") || originalname.endsWith("bmp")
                || originalname.endsWith("JPG") || originalname.endsWith("PNG") ||
                originalname.endsWith("JPEG") || originalname.endsWith("BMP")) {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            iv.setImageBitmap(bitmap);
            ab.setView(v);
        } else {
            ab.setMessage(getResources().getString(R.string.receive_message) + originalname);
        }

        ab.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final byte[] outputBytes = bytes;
        ab.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FileOutputStream out = null;
                File files = new File(Environment.getExternalStorageDirectory() + File.separator + "Hoylu");
                files.mkdir();
                File fileToSave = new File(files, originalname);
                try {
                    out = new FileOutputStream(fileToSave);
                    FileUtils.writeByteArrayToFile(fileToSave, outputBytes);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                dialog.dismiss();
            }
        });
        ab.create().show();
    }

    public void downloadFile(String filename, final String originalname) {
        String URL = ShareActivity.CONNECTION_STRING + "/file_for_download/:";
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet(URL + filename);
            HttpResponse response = httpClient.execute(request);
            InputStream input = null;
            byte[] bytes = null;

            try {
                input = response.getEntity().getContent();
                bytes = getBytes(input);

                final byte[] finalBytes = bytes;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAskingDialog(finalBytes, originalname);
                    }
                });
            } finally {
                if (input != null) try {
                    input.close();
                } catch (IOException logOrIgnore) {
                }
                socket.emit("fileReceived");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int n;
        baos.reset();
        while ((n = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }
}
