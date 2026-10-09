package com.example.demo.client.dummyjson;

import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.exception.ServicioExternoException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class DummyJsonClient {

    private final RestClient restClient;

    public DummyJsonClient(RestClient dummyJsonRestClient) {
        this.restClient = dummyJsonRestClient;
    }

    public List<DummyJsonProducto> obtenerProductos() {
        try {
            DummyJsonProductosResponse response = restClient.get()
                    .uri("/products?limit=0")
                    .retrieve()
                    .body(DummyJsonProductosResponse.class);

            if (response == null || response.products() == null) {
                throw new ServicioExternoException(
                        "DummyJSON devolvió una respuesta vacía al consultar productos", null);
            }

            return response.products();
        } catch (ServicioExternoException ex) {
            throw ex;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new ServicioExternoException("No se pudo consultar el catálogo de DummyJSON", ex);
        }
    }

    public DummyJsonProducto obtenerProductoPorId(Long id) {
        try {
            DummyJsonProducto producto = restClient.get()
                    .uri("/products/{id}", id)
                    .retrieve()
                    .body(DummyJsonProducto.class);

            if (producto == null) {
                throw new RecursoNoEncontradoException("No existe el producto con id " + id);
            }

            return producto;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe el producto con id " + id);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new ServicioExternoException("No se pudo consultar el producto " + id + " en DummyJSON", ex);
        }
    }
}
