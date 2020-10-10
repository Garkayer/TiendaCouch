package com.example.TiendaCouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {
    JSONArray datosJSON;
    JSONObject jsonObject;
    Integer posicion;
    ArrayList<String> arrayList =new ArrayList<String>();
    ArrayList<String> copyStringArrayList = new ArrayList<String>();
    ArrayAdapter<String> stringArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtenerDatosAmigos objObtenerAmigos = new obtenerDatosAmigos();
        objObtenerAmigos.execute();

        FloatingActionButton btnAgregarNuevoAmigos = findViewById(R.id.btnAgregarDatos);
        btnAgregarNuevoAmigos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agregarNuevosAmigos("nuevo", jsonObject);
            }
        });
        buscarAmigos();
    }
    void buscarAmigos(){
        final TextView tempVal = (TextView)findViewById(R.id.txtBuscarDatos);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                arrayList.clear();
                if( tempVal.getText().toString().trim().length()<1 ){//no hay texto para buscar
                    arrayList.addAll(copyStringArrayList);
                } else{//hacemos la busqueda
                    for (String amigo : copyStringArrayList){
                        if(amigo.toLowerCase().contains(tempVal.getText().toString().trim().toLowerCase())){
                            arrayList.add(amigo);
                        }
                    }
                }
                stringArrayAdapter.notifyDataSetChanged();
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_principal, menu);
        try {
            AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            posicion = adapterContextMenuInfo.position;
            menu.setHeaderTitle(datosJSON.getJSONObject(posicion).getString("nombre"));
        }catch (Exception ex){

        }
    }
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnxAgregarDatos:
                agregarNuevosAmigos("nuevo", jsonObject);
                return true;

            case R.id.mnxModificarDatos:
                try {
                    agregarNuevosAmigos("modificar", datosJSON.getJSONObject(posicion));
                }catch (Exception ex){}
                return true;

            case R.id.mnxEliminarDatos:

                AlertDialog eliminarFriend =  eliminarAmigo();
                eliminarFriend.show();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    private class obtenerDatosAmigos extends AsyncTask<Void,Void, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL("Http://192.168.1.18:5984/db_tienda/_design/Tienda/_view/Mi-Tienda");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String linea;
                while ((linea = reader.readLine()) != null) {
                    result.append(linea);
                }
            } catch (Exception ex) {
                //
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                jsonObject = new JSONObject(s);
                datosJSON = jsonObject.getJSONArray("rows");
                mostrarDatosAmigos();
            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, "Error la parsear los datos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    private void mostrarDatosAmigos(){
        ListView ltsAmigos = findViewById(R.id.ltsTiendaCouchDB);
        try {
            arrayList.clear();
            stringArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, arrayList);
            ltsAmigos.setAdapter(stringArrayAdapter);

            for (int i = 0; i < datosJSON.length(); i++) {
                stringArrayAdapter.add(datosJSON.getJSONObject(i).getJSONObject("value").getString("nombre"));
            }
            copyStringArrayList.clear();
            copyStringArrayList.addAll(arrayList);

            stringArrayAdapter.notifyDataSetChanged();
            registerForContextMenu(ltsAmigos);
        }catch (Exception ex){
            Toast.makeText(MainActivity.this, "Error al mostrar los datos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    public void agregarNuevosAmigos(String accion, JSONObject jsonObject){
        try {
            Bundle enviarParametros = new Bundle();
            enviarParametros.putString("accion",accion);
            enviarParametros.putString("dataAmigo",jsonObject.toString());

            Intent agregarAmigo = new Intent(MainActivity.this, agregar_datos.class);
            agregarAmigo.putExtras(enviarParametros);
            startActivity(agregarAmigo);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "Error al llamar agregar amigos: "+ e.toString(), Toast.LENGTH_LONG).show();
        }
    }
    AlertDialog eliminarAmigo(){
        AlertDialog.Builder confirmacion = new AlertDialog.Builder(MainActivity.this);
        try {
            confirmacion.setTitle(datosJSON.getJSONObject(posicion).getJSONObject("value").getString("nombre"));
            confirmacion.setMessage("Esta seguro de eliminar el registro?");
            confirmacion.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    eliminarDatosAmigo objEliminarAmigo = new eliminarDatosAmigo();
                    objEliminarAmigo.execute();

                    Toast.makeText(getApplicationContext(), "Amigo eliminado con exito.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
            confirmacion.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(), "Eliminacion cancelada por el usuario.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
        }catch (Exception ex){
            Toast.makeText(getApplicationContext(), "Error al mostrar la confoirmacion: "+ ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        return confirmacion.create();
    }
    private class eliminarDatosAmigo extends AsyncTask<String,String, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(String... parametros) {
            StringBuilder stringBuilder = new StringBuilder();
            String jsonResponse = null;
            try {
                URL url = new URL("http://192.168.1.18:5984/db_tienda/" +
                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id") + "?rev=" +
                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_rev"));

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("DELETE");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String inputLine;
                StringBuffer stringBuffer = new StringBuffer();
                while ((inputLine = reader.readLine()) != null) {
                    stringBuffer.append(inputLine + "\n");
                }
                if (stringBuffer.length() == 0) {
                    return null;
                }
                jsonResponse = stringBuffer.toString();
                return jsonResponse;
            } catch (Exception ex) {
                //
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                if (jsonObject.getBoolean("ok")) {
                    Toast.makeText(getApplicationContext(), "Datos de amigo guardado con exito", Toast.LENGTH_SHORT).show();
                    obtenerDatosAmigos objObtenerAmigos = new obtenerDatosAmigos();
                    objObtenerAmigos.execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Error al intentar guardar datos de amigo", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error al guardar amigo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}