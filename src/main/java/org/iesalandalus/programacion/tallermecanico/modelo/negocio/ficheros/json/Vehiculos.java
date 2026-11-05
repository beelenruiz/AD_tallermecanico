package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Vehiculo;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IVehiculos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehiculos implements IVehiculos {
    private static final String FICHERO_VEHICULOS = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "vehiculos.json");

    private static Vehiculos instancia;
    private ObjectMapper mapper = new ObjectMapper();

    private final File file = new File(FICHERO_VEHICULOS);

    private Vehiculos() {}

    static Vehiculos getInstancia() {
        if (instancia == null) {
            instancia = new Vehiculos();
        }
        return instancia;
    }

    @Override
    public void comenzar() {
        // nada
    }

    public List<Vehiculo> leer() {
        if (!file.exists()){
            return new ArrayList<>();
        }

        // leer lista de vehículos del fichero
        try {
            return mapper.readValue(file, new TypeReference<List<Vehiculo>>() {});
        } catch (IOException e ) {
            throw new IllegalStateException("No se ha podido leer el fichero de vehículos.", e);
        }
    }

    @Override
    public void terminar() {
        // nada
    }

    public void escribir(List<Vehiculo> vehiculos) {
        // crea las carpetas, si no existen
        file.getParentFile().mkdirs();
        // escribe la lista de vehículos en el fichero
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, vehiculos);
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido escribir en el fichero de vehículos.", e);
        }
    }

    @Override
    public List<Vehiculo> get() {
        return leer();
    }

    @Override
    public void insertar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede insertar un vehículo nulo.");

        // inserta vehículo, si no existe
        List<Vehiculo> vehiculos = leer();
        if (vehiculos.contains(vehiculo)) {
            throw new TallerMecanicoExcepcion("Ya existe un vehículo con esa matrícula.");
        }

        vehiculos.add(vehiculo);
        // actualiza fichero
        escribir(vehiculos);
    }

    @Override
    public Vehiculo buscar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "No se puede buscar un vehículo nulo.");

        // devuelve vehículo si existe
        List<Vehiculo> vehiculos = leer();
        int indice = vehiculos.indexOf(vehiculo);
        return (indice == -1) ? null : vehiculos.get(indice);
    }

    @Override
    public void borrar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede borrar un vehículo nulo.");

        // elimina el vehículo del fichero, si existe
        List<Vehiculo> vehiculos = leer();
        if (!vehiculos.remove(vehiculo)) {
            throw new TallerMecanicoExcepcion("No existe ningún vehículo con esa matrícula.");
        }

        // actualiza fichero
        escribir(vehiculos);
    }
}
