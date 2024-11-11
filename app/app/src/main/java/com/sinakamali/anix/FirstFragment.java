package com.sinakamali.anix;

import static android.content.Context.MODE_PRIVATE;
import static com.sinakamali.anix.anixCore.AnixCore.NOT_FOUND_ERROR;
import static com.sinakamali.anix.anixCore.AnixCore.SAVED_KEYPAIR_KEY;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.sinakamali.anix.databinding.FragmentFirstBinding;
import com.sinakamali.anix.anixCore.AnixCore;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    SharedPreferences sharedPref;

    private final String SAVED_MAC_ADDRESS_KEY = "saved_dst_mac";

    public AnixCore internal_anixCore;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        sharedPref = requireActivity().getSharedPreferences("main", MODE_PRIVATE);

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    private void refreshDstMacTextHeader() {
        String dstMac = sharedPref.getString(SAVED_MAC_ADDRESS_KEY, "Nothing has been set yet!");
        String header = getString(R.string.mac_header_text, dstMac);
        binding.textviewFirst.setText(header);
    }

    private void refreshKeyPairTextHeader(View view) {
        String keypair = sharedPref.getString(AnixCore.SAVED_KEYPAIR_KEY, NOT_FOUND_ERROR);
        if (keypair.equals(NOT_FOUND_ERROR)) {
            try {
                internal_anixCore = new AnixCore();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            keypair = internal_anixCore.getKeyPairString();
            setKeyPair(view, keypair);
        } else {
            try {
                internal_anixCore = new AnixCore(keypair);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("pubkey: " + internal_anixCore.getPublicKeyString());
        String keypair_header = getString(R.string.keypair_header_text, keypair);
        binding.keypairHeaderText.setText(keypair_header);
    }

    private void setMac(View view) {
        sharedPref.edit().putString(SAVED_MAC_ADDRESS_KEY, String.valueOf(binding.editTextText.getText())).apply();
        refreshDstMacTextHeader();
    }

    private void setKeyPair(View view, String keypair) {
        sharedPref.edit().putString(SAVED_KEYPAIR_KEY, keypair).apply();
//        refreshDstMacTextHeader();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );

        refreshDstMacTextHeader();
        refreshKeyPairTextHeader(view);


        binding.setMacButton.setOnClickListener(this::setMac);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}