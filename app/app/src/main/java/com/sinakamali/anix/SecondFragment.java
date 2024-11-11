package com.sinakamali.anix;

import static android.content.Context.MODE_PRIVATE;
import static com.sinakamali.anix.anixCore.AnixCore.NOT_FOUND_ERROR;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.sinakamali.anix.databinding.FragmentSecondBinding;
import com.sinakamali.anix.anixCore.KeyManager;
import com.sinakamali.anix.anixCore.AnixCore;
import com.sinakamali.anix.anixCore.AnixCoreMessage;
import com.sinakamali.anix.anixCore.PSU;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.internal.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.jcajce.interfaces.EdDSAPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class SecondFragment extends Fragment {

    SharedPreferences sharedPref;
    private FragmentSecondBinding binding;
    private AnixCore internalAnixCore;
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private void doCryptoTest(View view) {
        StringBuilder messageText = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            messageText.append("*");
        }

        byte[] messageBytes = messageText.toString().getBytes();

        StringBuilder tempMessageText = new StringBuilder();
        for (int i = 0; i < 320; i++) {
            tempMessageText.append("*");
        }

        byte[] tempMessageBytes = tempMessageText.toString().getBytes();

        long start, end;
        String timeTook;

        Toast.makeText(getActivity(), "starting test...", Toast.LENGTH_SHORT).show();
        try {
//            AnixCoreMessage message = internalAnixCore.createMessage(messageText.toString().getBytes());

            System.out.println("len of message bytes: " + tempMessageBytes.length);
            byte[] tempEncryptedMessage = KeyManager.encryptMessage(internalAnixCore.keyManager.getCurrEncyptionPublicKey(), tempMessageBytes);
            System.out.println("len of encrypted message bytes: " + tempEncryptedMessage.length);

            // Test new create signature
//            Security.addProvider(new BouncyCastleProvider());
            byte[] privateKeyBytes = Base64.getUrlDecoder().decode("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A");
            byte[] publicKeyBytes = Base64.getUrlDecoder().decode("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo");
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME);

            PrivateKeyInfo privKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(privateKeyBytes));
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privKeyInfo.getEncoded());
            // =========== Keys =========
            PrivateKey jcaPrivateKey = keyFactory.generatePrivate(pkcs8KeySpec);
            org.bouncycastle.jcajce.interfaces.EdDSAPublicKey jcaPublicKey = ((EdDSAPrivateKey) jcaPrivateKey).getPublicKey();

            start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                KeyManager.signMessageEdDSA(messageBytes, jcaPrivateKey);
                if (i % 5000 == 0) {
                    System.out.println((i / 5000) * 50 + "% done");
                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
                }
            }
            end = System.nanoTime();
            timeTook = String.valueOf((end - start) / 1000);
            Toast.makeText(getActivity(), "ending test sign messages EdDSA (us): " + timeTook, Toast.LENGTH_SHORT).show();
            System.out.println("ending test sign messages EdDSA (us): " + timeTook);


            // Test new verify signature
            byte[] signatureEdDSA = KeyManager.signMessageEdDSA(messageBytes, jcaPrivateKey);
            start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                KeyManager.verifyMessageEdDSA(messageBytes, signatureEdDSA, jcaPublicKey);
                if (i % 5000 == 0) {
                    System.out.println((i / 5000) * 50 + "% done");
                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
                }
            }
            end = System.nanoTime();
            timeTook = String.valueOf((end - start) / 1000);
            Toast.makeText(getActivity(), "ending test verify signatures EdDSA (us): " + timeTook, Toast.LENGTH_SHORT).show();
            System.out.println("ending test verify signatures EdDSA (us): " + timeTook);


            // Test new create blinded signature
            BigInteger L = new BigInteger("2");
            L = L.pow(252);
            BigInteger b = new BigInteger("27742317777372353535851937790883648493");
            L = L.add(b);
            start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                PrivateKey blindedPrivateKey = KeyManager.getBlindedPrivateKey(messageBytes, privateKeyBytes, publicKeyBytes);
                KeyManager.signMessageEdDSA(messageBytes, blindedPrivateKey);
                if (i % 5000 == 0) {
                    System.out.println((i / 5000) * 50 + "% done");
                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
                }
            }
            end = System.nanoTime();
            timeTook = String.valueOf((end - start) / 1000);
            Toast.makeText(getActivity(), "ending test blinded sign messages EdDSA (us): " + timeTook, Toast.LENGTH_SHORT).show();
            System.out.println("ending test blinded sign messages EdDSA (us): " + timeTook);


            // Test new verify blinded signature
            PrivateKey blindedPrivateKey = KeyManager.getBlindedPrivateKey(messageBytes, privateKeyBytes, publicKeyBytes);
            org.bouncycastle.jcajce.interfaces.EdDSAPublicKey blindedPublicKey = ((EdDSAPrivateKey) blindedPrivateKey).getPublicKey();
            byte[] signatureBlindedEdDSA = KeyManager.signMessageEdDSA(messageBytes, blindedPrivateKey);
            start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                PublicKey taylorsBlindedPublicKey = KeyManager.getBlindedPublicKey(messageBytes, publicKeyBytes);
                KeyManager.verifyMessageEdDSA(messageBytes, signatureBlindedEdDSA, blindedPublicKey);
                if (i % 5000 == 0) {
                    System.out.println((i / 5000) * 50 + "% done");
                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
                }
            }
            end = System.nanoTime();
            timeTook = String.valueOf((end - start) / 1000);
            Toast.makeText(getActivity(), "ending test verify blinded signatures EdDSA (us): " + timeTook, Toast.LENGTH_SHORT).show();
            System.out.println("ending test verify blinded signatures EdDSA (us): " + timeTook);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doCreateObjectTest(View view) {
        StringBuilder messageText = new StringBuilder();
        for (int i = 0; i < 255; i++) {
            messageText.append("*");
        }

        long start, end;
        String timeTook;

        try {
            // Message Creation
            PSU temPsu = internalAnixCore.keyManager.generateNewPSU();
            start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                // New version
                AnixCoreMessage message = internalAnixCore.createMessage(messageText.toString().getBytes(), temPsu);
                if (i % 5000 == 0) {
                    System.out.println((i / 5000) * 50 + "% done");
                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
                }
            }
            end = System.nanoTime();
            timeTook = String.valueOf((end - start) / 1000);
            Toast.makeText(getActivity(), "ending test message creation (us): " + timeTook, Toast.LENGTH_SHORT).show();
            System.out.println("ending test message creation (us): " + timeTook);

            // Message Voting
//            AnixCoreMessage message = internalAnixCore.createMessage(messageText.toString().getBytes());
//            start = System.nanoTime();
//            for (int i = 0; i < 10000; i++) {
//                internalAnixCore.voteOnMessage(message, true);
//                if (i % 5000 == 0) {
//                    System.out.println((i / 5000) * 50 + "% done");
//                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
//                }
//            }
//            end = System.nanoTime();
//            timeTook = String.valueOf((end - start)/1000);
//            Toast.makeText(getActivity(), "ending test message voting (us): " + timeTook, Toast.LENGTH_SHORT).show();
//            System.out.println("ending test message voting (us): " + timeTook);

            // PSU Creation
//            AnixCore test_anixCore = new AnixCore();
//            start = System.nanoTime();
//            for (int i = 0; i < 10000; i++) {
//                test_anixCore.keyManager.generateNewPSU();
//                if (i % 5000 == 0) {
//                    System.out.println((i / 5000) * 50 + "% done");
//                    Toast.makeText(getActivity(), (i / 5000) * 50 + "% done", Toast.LENGTH_SHORT).show();
//                }
//            }
//            end = System.nanoTime();
//            timeTook = String.valueOf((end - start)/1000);
//            Toast.makeText(getActivity(), "ending test PSU generation (us): " + timeTook, Toast.LENGTH_SHORT).show();
//            System.out.println("ending test PSU generation (us): " + timeTook);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void sendMessagesOverBluetooth(View view) {

        StringBuilder messageText = new StringBuilder();
        for (int i = 0; i < 140; i++) {
            messageText.append("a");
        }

        String SAVED_MAC_ADDRESS_KEY = "saved_dst_mac";
        String dstMac = sharedPref.getString(SAVED_MAC_ADDRESS_KEY, "Nothing has been set yet!").trim();
        long bytesRead = 0;

        try {
            AnixCoreMessage message = internalAnixCore.createMessage(messageText.toString().getBytes());
            byte[] messageByteArray = message.dumpMessageToBytes();
            System.out.println(message.dumpMessageToString());
            System.out.println("message size is " + messageByteArray.length + "bytes");
            message = internalAnixCore.voteOnMessage(message, true);
            byte[] voteByteArray = message.dumpVotesToBytes();
            System.out.println(new String(voteByteArray, StandardCharsets.UTF_8));
            System.out.println("vote size is " + voteByteArray.length + "bytes");


            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(dstMac);
            if (device == null) {
                System.out.println("device was null!");
            } else {
                System.out.println("device was not null!");
            }

            assert device != null;
            BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            long connectionStart = System.currentTimeMillis();
            long connectionEnd = 0;
            assert socket != null;
            socket.connect();
            System.out.println("connected!");
            OutputStream outputStream = socket.getOutputStream();
            if (outputStream == null) {
                System.out.println("stream was null!");
            } else {
                System.out.println("stream was not null!");
            }

            connectionEnd = System.currentTimeMillis();

            System.out.println("Establishing the connection 1 took: (ms)" + (connectionEnd - connectionStart));

            ByteArrayOutputStream internalByteStream = new ByteArrayOutputStream();
            int MESSAGE_COUNT = 100;
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                internalByteStream.write(messageByteArray);
            }

            int VOTE_COUNT = 10000;
            for (int i = 0; i < VOTE_COUNT; i++) {
                internalByteStream.write(voteByteArray);
            }

            System.out.println("total size of data to send: " + internalByteStream.toByteArray().length);

            long start = System.currentTimeMillis();

            outputStream.write(internalByteStream.toByteArray());


            outputStream.flush();
            outputStream.close();

            // Receiving ===========


            BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Anix", MY_UUID);

            connectionStart = System.currentTimeMillis();
            connectionEnd = 0;

            System.out.println("listening for connection...");
            socket = serverSocket.accept(); // Blocking call, waits until connection is established
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            boolean first_impact = true;
            try {
                while (true) {
                    bytes = inputStream.read(buffer); // This call will block until there's something to read
                    bytesRead += bytes;
//                String incomingMessage = new String(buffer, 0, bytes);
                    if (first_impact) {
                        connectionEnd = System.currentTimeMillis();
                        Toast.makeText(getActivity(), "GOT MESSAGE!!!", Toast.LENGTH_SHORT).show();
                        System.out.println("Got message!");
                        first_impact = false;
                    }
                    // Process the incoming message as needed
                }
            } catch (Exception e) {
                System.out.println("out of receiving");
            }

            long end = System.currentTimeMillis();
            System.out.println("Sending and receiving everything took: (ms)" + (end - start));
            System.out.println("Establishing the connection took: (ms)" + (connectionEnd - connectionStart));

            socket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void receiveMessagesOverBluetooth(View view) {
        BluetoothServerSocket serverSocket = null;

        long start = 0, end;
        long bytesRead = 0;

        StringBuilder messageText = new StringBuilder();
        for (int i = 0; i < 140; i++) {
            messageText.append("a");
        }

        String SAVED_MAC_ADDRESS_KEY = "saved_dst_mac";
        String dstMac = sharedPref.getString(SAVED_MAC_ADDRESS_KEY, "Nothing has been set yet!").trim();


        try {

            AnixCoreMessage message = internalAnixCore.createMessage(messageText.toString().getBytes());
            byte[] messageByteArray = message.dumpMessageToBytes();
            System.out.println(message.dumpMessageToString());
            System.out.println("message size is " + messageByteArray.length + "bytes");
            message = internalAnixCore.voteOnMessage(message, true);
            byte[] voteByteArray = message.dumpVotesToBytes();
            System.out.println(new String(voteByteArray, StandardCharsets.UTF_8));
            System.out.println("vote size is " + voteByteArray.length + "bytes");

            ByteArrayOutputStream internalByteStream = new ByteArrayOutputStream();
            int MESSAGE_COUNT = 100;
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                internalByteStream.write(messageByteArray);
            }

            int VOTE_COUNT = 10000;
            for (int i = 0; i < VOTE_COUNT; i++) {
                internalByteStream.write(voteByteArray);
            }

            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Anix", MY_UUID);
            System.out.println("listening for connection...");
            BluetoothSocket socket = serverSocket.accept(); // Blocking call, waits until connection is established
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            start = System.currentTimeMillis();
            boolean first_impact = true;
            try {
                while (true) {
                    bytes = inputStream.read(buffer); // This call will block until there's something to read
                    bytesRead += bytes;
//                String incomingMessage = new String(buffer, 0, bytes);
                    if (first_impact) {
                        Toast.makeText(getActivity(), "GOT MESSAGE!!!", Toast.LENGTH_SHORT).show();
                        System.out.println("Got message!");
                        first_impact = false;
                    }
                    // Process the incoming message as needed
                }
            } catch (IOException e) {
                System.out.println("out of receiving");
                socket.close();
            }


            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(dstMac);

            assert device != null;
            socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            assert socket != null;
            socket.connect();
            System.out.println("connected!");
            OutputStream outputStream = socket.getOutputStream();
            if (outputStream == null) {
                System.out.println("stream was null!");
            } else {
                System.out.println("stream was not null!");
            }

            System.out.println("total size of data to send: " + internalByteStream.toByteArray().length);

            outputStream.write(internalByteStream.toByteArray());


            outputStream.flush();
            outputStream.close();


            end = System.currentTimeMillis();
            System.out.println("Sending and receiving everything took: (ms)" + (end - start));

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (serverSocket != null) {
                try {
                    System.out.println("I'm hereeeee");

                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);

        sharedPref = requireActivity().getSharedPreferences("main", MODE_PRIVATE);

        return binding.getRoot();

    }

    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            return;
        }
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupBouncyCastle();

        binding.prevButton2.setOnClickListener(v ->
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment)
        );

        String keypair = sharedPref.getString(AnixCore.SAVED_KEYPAIR_KEY, NOT_FOUND_ERROR);
        try {
            internalAnixCore = new AnixCore(keypair);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        binding.cryptobutton.setOnClickListener(this::doCryptoTest);
        binding.componentbutton.setOnClickListener(this::doCreateObjectTest);
        binding.messagebutton.setOnClickListener(this::sendMessagesOverBluetooth);
        binding.receivebutton.setOnClickListener(this::receiveMessagesOverBluetooth);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("hello world!!!");
    }
}