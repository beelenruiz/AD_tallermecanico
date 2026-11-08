package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.*;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ITrabajos;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class Trabajos implements ITrabajos {
    private static final String FICHERO_TRABAJOS = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "trabajos.json");

    private static Trabajos instancia;
    private ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final File file = new File(FICHERO_TRABAJOS);

    private Trabajos() {}

    static Trabajos getInstancia() {
        if (instancia == null) {
            instancia = new Trabajos();
        }
        return instancia;
    }

    @Override
    public void comenzar() {
        // nada
    }

    public List<Trabajo> leer(){
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        // leer la lista de trabajo del fichero
        try {
            return mapper.readValue(file, new TypeReference<List<Trabajo>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido leer el fichero de trabajos.", e);
        }
    }

    @Override
    public void terminar() {
        // nada
    }

    public void escribir(List<Trabajo> trabajos) {
        // crea las carpetas, si no existen
        file.getParentFile().mkdirs();
        // escribe la lista de trabajos en el fichero
        try {
            mapper.writerFor(new TypeReference<List<Trabajo>>() {})
                    .withDefaultPrettyPrinter()
                    .writeValue(file, trabajos);
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido escribir en el fichero de trabajos.", e);
        }
    }

    @Override
    public List<Trabajo> get() {
        return leer();
    }

    @Override
    public List<Trabajo> get(Cliente cliente) {
        Objects.requireNonNull(cliente, "No se pueden buscar trabajos de un cliente nulo.");

        List<Trabajo> trabajos = leer();
        List<Trabajo> trabajosCliente = new ArrayList<>();

        // si el trabajo pertenece al cliente lo añado a la lista
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getCliente().equals(cliente)) trabajosCliente.add(trabajo);
        }

        return trabajosCliente;
    }

    @Override
    public List<Trabajo> get(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "No se pueden buscar trabajos de un vehículo nulo.");

        List<Trabajo> trabajos = leer();
        List<Trabajo> trabajosVehiculo = new ArrayList<>();

        // si el trabajo pertenece al vehículo lo añado a la lista
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getVehiculo().equals(vehiculo)) trabajosVehiculo.add(trabajo);
        }

        return trabajosVehiculo;
    }

    @Override
    public Map<TipoTrabajo, Integer> getEstadisticasMensuales(LocalDate mes) {
        Objects.requireNonNull(mes, "El mes no puede ser nulo.");

        List<Trabajo> trabajos = leer();
        Map<TipoTrabajo, Integer> estadisticas = inicializarEstadisticas();

        // recorre todos los trabajos
        for (Trabajo trabajo : trabajos) {
            LocalDate fecha = trabajo.getFechaInicio();
            // si la fecha coincide con la que nos pasan, mira el tipo y lo suma
            if (fecha.getMonthValue() == mes.getMonthValue() && fecha.getYear() == mes.getYear()) {
                TipoTrabajo tipoTrabajo = TipoTrabajo.get(trabajo);
                estadisticas.put(tipoTrabajo, estadisticas.get(tipoTrabajo) + 1);
            }
        }

        return estadisticas;
    }

    private Map<TipoTrabajo, Integer> inicializarEstadisticas() {
        // crea el mapa con trabajos mecanicos y de revision a 0
        Map<TipoTrabajo, Integer> estadisticas = new EnumMap<>(TipoTrabajo.class);
        for (TipoTrabajo tipoTrabajo : TipoTrabajo.values()) {
            estadisticas.put(tipoTrabajo, 0);
        }
        return estadisticas;
    }

    @Override
    public void insertar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede insertar un trabajo nulo.");

        // inserta trabajo, si no existe
        List<Trabajo> trabajos = leer();
        if (trabajos.contains(trabajo)) {
            throw new TallerMecanicoExcepcion("El trabajo a insertar ya existe.");
        }
        comprobarTrabajo(trabajos, trabajo.getCliente(), trabajo.getVehiculo(), trabajo.getFechaInicio());

        trabajos.add(trabajo);
        // actualiza fichero
        escribir(trabajos);
    }

    private void comprobarTrabajo(List<Trabajo> trabajos, Cliente cliente, Vehiculo vehiculo, LocalDate fechaInicio) throws TallerMecanicoExcepcion {
        for (Trabajo trabajo : trabajos) {
            if (!trabajo.estaCerrado()) {
                // ya hay un trabajo activo para el cliente o vehiculo
                if (trabajo.getCliente().equals(cliente)) {
                    throw new TallerMecanicoExcepcion("El cliente tiene otro trabajo en curso.");
                } else if (trabajo.getVehiculo().equals(vehiculo)) {
                    throw new TallerMecanicoExcepcion("El vehículo está actualmente en el taller.");
                }
            } else {
                // hay un trabajo cerrado anterior para el cliente o vehiculo
                if (trabajo.getCliente().equals(cliente) && !fechaInicio.isAfter(trabajo.getFechaFin())) {
                    throw new TallerMecanicoExcepcion("El cliente tiene otro trabajo posterior.");
                } else if (trabajo.getVehiculo().equals(vehiculo) && !fechaInicio.isAfter(trabajo.getFechaFin())) {
                    throw new TallerMecanicoExcepcion("El vehículo tiene otro trabajo posterior.");
                }
            }
        }
    }

    private Trabajo getTrabajoAbierto(List<Trabajo> trabajos, Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No puedo operar sobre un vehículo nulo.");

        Trabajo trabajoEncontrado = null;

        // busco un trabajo abierto que corresponda al vehículo del trabajo que buscamos
        for (Trabajo trabajo : trabajos) {
            if (trabajoEncontrado == null && trabajo.getVehiculo().equals(vehiculo) && !trabajo.estaCerrado()) {
                trabajoEncontrado = trabajo;
            }
        }

        if (trabajoEncontrado == null) throw new TallerMecanicoExcepcion("No existe ningún trabajo abierto para ese vehículo.");
        return trabajoEncontrado;
    }

    @Override
    public Trabajo anadirHoras(Trabajo trabajo, int horas) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede añadir horas a un trabajo nulo.");

        // busco un trabajo abierto que corresponda al vehiculo del trabajo que nos pasan
        List<Trabajo> trabajos = leer();
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajos, trabajo.getVehiculo());
        trabajoEncontrado.anadirHoras(horas);

        // guardar cambios
        escribir(trabajos);
        return trabajoEncontrado;
    }

    @Override
    public Trabajo anadirPrecioMaterial(Trabajo trabajo, float precioMaterial) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede añadir precio del material a un trabajo nulo.");

        // busco un trabajo abierto que corresponda al vehiculo del trabajo que nos pasan
        List<Trabajo> trabajos = leer();
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajos, trabajo.getVehiculo());
        if (trabajoEncontrado instanceof Mecanico mecanico) {
            mecanico.anadirPrecioMaterial(precioMaterial);
        } else {
            throw new TallerMecanicoExcepcion("No se puede añadir precio del material a este tipo de trabajo.");
        }

        // guardar cambios
        escribir(trabajos);

        return trabajoEncontrado;
    }

    @Override
    public Trabajo cerrar(Trabajo trabajo, LocalDate fechaFin) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede cerrar un trabajo nulo.");

        // busco un trabajo abierto que corresponda al vehiculo del trabajo que nos pasan
        List<Trabajo> trabajos = leer();
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajos, trabajo.getVehiculo());
        trabajoEncontrado.cerrar(fechaFin);

        // guardar trabajos
        escribir(trabajos);

        return trabajoEncontrado;
    }

    @Override
    public Trabajo buscar(Trabajo trabajo) {
        Objects.requireNonNull(trabajo, "No se puede buscar un trabajo nulo.");

        // devuelve el trabajo, si existe
        List<Trabajo> trabajos = leer();
        int indice = trabajos.indexOf(trabajo);
        return (indice == -1) ? null : trabajos.get(indice);
    }

    @Override
    public void borrar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede borrar un trabajo nulo.");

        // elimina el trabajo del fichero, si existe
        List<Trabajo> trabajos = leer();
        if (!trabajos.remove(trabajo)) {
            throw new TallerMecanicoExcepcion("El trabajo a eliminar no existe.");
        }

        // actualiza fichero
        escribir(trabajos);
    }
}
