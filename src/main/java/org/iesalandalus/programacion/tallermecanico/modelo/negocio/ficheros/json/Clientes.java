package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Cliente;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IClientes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Clientes implements IClientes {
    private static final String FICHERO_CLIENTES = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "clientes.json");

    private static Clientes instancia;
    private ObjectMapper mapper = new ObjectMapper();

    private final File file = new File(FICHERO_CLIENTES);

    private Clientes() {}

    static Clientes getInstancia() {
        if (instancia == null) {
            instancia = new Clientes();
        }
        return instancia;
    }

    @Override
    public void comenzar() {
        // nada
    }

    public List<Cliente> leer() {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        // leer lista de clientes del fichero
        try {
            return mapper.readValue(file, new TypeReference<List<Cliente>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido leer el fichero de clientes.", e);
        }
    }

    @Override
    public void terminar() {
        // nada
    }

    public void escribir(List<Cliente> clientes){
        // crea las carpetas, si no existen
        file.getParentFile().mkdirs();
        // escribe la lista de clientes en el fichero
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, clientes);
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido escribir en el fichero de clientes.", e);
        }
    }

    @Override
    public List<Cliente> get() {
        return leer();
    }

    @Override
    public void insertar(Cliente cliente) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede insertar un cliente nulo.");

        // inserta clientes, si no existe
        List<Cliente> clientes = leer();
        if (clientes.contains(cliente)) {
            throw new TallerMecanicoExcepcion("Ya existe un cliente con ese DNI.");
        }

        clientes.add(cliente);
        // actualiza fichero
        escribir(clientes);
    }

    @Override
    public Cliente modificar(Cliente cliente, String nombre, String telefono) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede modificar un cliente nulo.");

        // busca cliente a actualizar
        List<Cliente> clientes = leer();
        int indice = clientes.indexOf(cliente);
        if (indice == -1) {
            throw new TallerMecanicoExcepcion("No existe ningún cliente con ese DNI.");
        }
        Cliente clienteEncontrado = clientes.get(indice);

        // actualiza nombre y/o telefono del cliente
        if (nombre != null && !nombre.isBlank()) {
            clienteEncontrado.setNombre(nombre);
        }
        if (telefono != null && !telefono.isBlank()) {
            clienteEncontrado.setTelefono(telefono);
        }

        // actualiza fichero
        escribir(clientes);
        return clienteEncontrado;
    }

    @Override
    public Cliente buscar(Cliente cliente) {
        Objects.requireNonNull(cliente, "No se puede buscar un cliente nulo.");

        List<Cliente> clientes = leer();

        // devuelve cliente si existe
        int indice = clientes.indexOf(cliente);
        return (indice == -1) ? null : clientes.get(indice);
    }

    @Override
    public void borrar(Cliente cliente) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede borrar un cliente nulo.");

        // elimina al cliente del fichero, si existe
        List<Cliente> clientes = leer();
        if (!clientes.remove(cliente)) {
            throw new TallerMecanicoExcepcion("No existe ningún cliente con ese DNI.");
        }

        // actualiza fichero
        escribir(clientes);
    }
}
