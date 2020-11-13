package com.ugd9_b_9978.Views;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.ugd9_b_9978.API.TransaksiBukuAPI;
import com.ugd9_b_9978.Models.DTBuku;
import com.ugd9_b_9978.Models.TransaksiBuku;
import com.ugd9_b_9978.Adapters.AdapterTransaksiBuku;
import com.ugd9_b_9978.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static com.android.volley.Request.Method.GET;

public class ViewsCart extends Fragment{

    private RecyclerView recyclerView;
    private TextView tvTotalBiaya;
    private Button btnBayar;
    private CheckBox checkBox;
    private CardView panelBayar, panelCheckBox;
    private AdapterTransaksiBuku adapter;
    private List<TransaksiBuku> transaksiBukuList;
    private List<DTBuku> listDTBuku;
    private View view;
    public Boolean isFullChecked;
    private int ori, grid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_views_cart, container, false);

        init();
        setAdapter();
        getTransaksi();
        setAttribut();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(menu.findItem(R.id.btnSearch) != null)
            menu.findItem(R.id.btnSearch).setVisible(false);
        if(menu.findItem(R.id.btnAdd) != null)
            menu.findItem(R.id.btnAdd).setVisible(false);
    }

    private void init() {
        checkBox        = view.findViewById(R.id.checkBox);
        tvTotalBiaya    = view.findViewById(R.id.totalBiaya);
        btnBayar        = view.findViewById(R.id.btnBayar);
        panelBayar      = view.findViewById(R.id.panelBayar);
        panelCheckBox   = view.findViewById(R.id.panelCheckBox);
        recyclerView    = view.findViewById(R.id.recycler_view);
        panelCheckBox.setVisibility(View.GONE);
        setVisiblePanelBayar(View.GONE);
    }

    private void setAttribut() {
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    setVisiblePanelBayar(View.VISIBLE);
                    setChecked(true);
                }
                else
                {
                    setVisiblePanelBayar(View.GONE);
                    if(isFullChecked)
                        setChecked(false);
                }
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public void setAdapter(){
        getActivity().setTitle("Cart");
        listDTBuku = new ArrayList<>();
        transaksiBukuList = new ArrayList<>();
        adapter = new AdapterTransaksiBuku(view.getContext(), transaksiBukuList,
                new AdapterTransaksiBuku.OnQuantityChangeListener() {
                    @Override
                    public void onQuantityChange(Double totalBiaya, Double subTotal, Boolean full, Boolean empty) {
                        isFullChecked = full;
                        if(!empty){
                            setVisiblePanelBayar(View.VISIBLE);
                            NumberFormat formatter = new DecimalFormat("#,###");
                            tvTotalBiaya.setText("Rp "+ formatter.format(totalBiaya));
                        }

                        if(full)
                            checkBox.setChecked(true);
                        else
                            checkBox.setChecked(false);
                    }
                });

        ori = getResources().getConfiguration().orientation;
        grid = 1;
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            grid = 2;
        }

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(view.getContext(),grid);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    public void setChecked(Boolean bool){
        for (int i = 0; i < transaksiBukuList.size(); i++) {
            transaksiBukuList.get(i).isChecked = bool;
            for (int j = 0; j < transaksiBukuList.get(i).getDtBukuList().size(); j++) {
                transaksiBukuList.get(i).getDtBukuList().get(j).isChecked = bool;
            }
        }
    }

    public void setVisiblePanelBayar(int visible){
        panelBayar.setVisibility(visible);
        if(visible == View.VISIBLE)
            recyclerView.getLayoutParams().height = (int) getResources().getDimension(R.dimen.panel_height);
        else
            recyclerView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public void getTransaksi() {
        //Tambahkan tampil transaksi buku disini
        //Pendeklarasian queue
        RequestQueue queue = Volley.newRequestQueue(view.getContext());

        //Meminta tanggapan string dari URL yang telah disediakan menggunakan method GET
        //untuk request ini tidak memerlukan parameter
        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage("loading....");
        progressDialog.setTitle("Menampilkan data cart");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        final JsonObjectRequest stringRequest = new JsonObjectRequest(GET, TransaksiBukuAPI.URL_SELECT
                , null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //Disini bagian jika response jaringan berhasil tidak terdapat ganguan/error
                progressDialog.dismiss();
                try {
                    //Mengambil data response json object yang berupa data mahasiswa
                    JSONArray jsonArray = response.getJSONArray("transaksibuku");

                    if(!transaksiBukuList.isEmpty())
                        transaksiBukuList.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        //Mengubah data jsonArray tertentu menjadi json Object
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                        String noTransaksi      = jsonObject.optString("noTransaksi");
                        int idToko      = jsonObject.optInt("idToko");
                        Double totalBiaya = jsonObject.optDouble("totalBiaya");
                        String tglTransaksi              = jsonObject.optString("tglTransaksi");
                        String namaToko             = jsonObject.optString("namaToko");

                        if(!listDTBuku.isEmpty())
                            listDTBuku.clear();

                        JSONArray dtBukuArr = jsonObject.getJSONArray("dtbuku");
                        for (int j=0 ; j < dtBukuArr.length(); j++){
                            JSONObject dtBukuOBJ = (JSONObject) dtBukuArr.get(j);
                            int idBuku = dtBukuOBJ.optInt("idBuku");
                            int jumlah = dtBukuOBJ.optInt("jumlah");
                            String namaBuku = dtBukuOBJ.optString("namaBuku");
                            //String pengarang = dtBukuOBJ.optString("namaBuku");
                            Double harga = dtBukuOBJ.optDouble("harga");
                            String gambar = dtBukuOBJ.optString("gambar");

                            DTBuku dtBuku = new DTBuku(idBuku,noTransaksi,jumlah,namaBuku,harga,gambar);
                            Log.i("idBuku","id : "+idBuku);

                            listDTBuku.add(dtBuku);
                        }

                        TransaksiBuku transaksiBuku = new TransaksiBuku(noTransaksi,idToko,tglTransaksi,totalBiaya,namaToko,listDTBuku);

                        //Menambahkan objek user tadi ke list user
                        transaksiBukuList.add(transaksiBuku);
                        Log.i("No transaksi : ",transaksiBuku.getNoTransaksi());
                    }
                    adapter.notifyDataSetChanged();
                }catch (JSONException e){
                    e.printStackTrace();
                }
                Toast.makeText(view.getContext(), response.optString("message"),
                        Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Disini bagian jika response jaringan terdapat ganguan/error
                progressDialog.dismiss();
                Toast.makeText(view.getContext(), error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        //Disini proses penambahan request yang sudah kita buat ke reuest queue yang sudah dideklarasi
        queue.add(stringRequest);
    }
}