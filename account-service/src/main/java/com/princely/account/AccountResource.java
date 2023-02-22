package com.princely.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.logging.Log;
import org.jboss.resteasy.annotations.Body;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Path("/accounts")
public class AccountResource {
    private final Set<Account> accounts = new HashSet<>();

    @PostConstruct
    public void setup() {
        accounts.add(new Account(123456789L, 987654321L, "George Baird", new
                BigDecimal("354.23")));
        accounts.add(new Account(121212121L, 888777666L, "Mary Taylor", new
                BigDecimal("560.03")));
        accounts.add(new Account(545454545L, 222444999L, "Diana Rigg", new
                BigDecimal("422.00")));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Account> allAccounts() {
        return accounts;
    }

    @GET
    @Path("/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(@PathParam("accountNumber") long accountNumber) {
        Optional<Account> result = accounts.stream()
                .filter(account -> account.getAccountNumber() == accountNumber)
                .findFirst();
        return result.orElseThrow(() ->
                new WebApplicationException("Account with account number "
                        + accountNumber + " not found.", 404));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(Account account) {
        if (account.getAccountNumber() == null) {
            throw new WebApplicationException("Account number cannot be null.", 400);
        }
        accounts.add(account);
        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @PUT
    @Path("/{accountNumber}/withdraw")
    public Response withdraw(@PathParam("accountNumber") long accountNumber, String amount) {
        Account account = getAccount(accountNumber);
        BigDecimal bigDecimal = new BigDecimal(amount.trim());
        if (account.getBalance().compareTo(bigDecimal) < 0) {
            throw new WebApplicationException("Insufficient funds.", 400);
        }
        account.withdrawFunds(bigDecimal);
        return Response.ok(account).build();
    }

    @PUT
    @Path("/{accountNumber}/deposit")
    public Response deposit(@PathParam("accountNumber") long accountNumber, String amount) {
        Account account = getAccount(accountNumber);
        Log.info("Depositing " + amount + " into account " + accountNumber);
        account.addFunds(new BigDecimal(amount));
        return Response.ok(account).build();
    }

    @DELETE
    @Path("/{accountNumber}")
    public Response deleteAccount(@PathParam("accountNumber") long accountNumber) {
        Account account = getAccount(accountNumber);
        accounts.remove(account);
        return Response.noContent().build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception)
                        .getResponse().getStatus(); 
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode response = mapper.createObjectNode();
                    response
                            .put("exceptionType", exception.getClass().getName())
                            .put("code", code);

            if (exception.getMessage() != null) {
                response.put("error", exception.getMessage());
            }
            try {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                return Response.status(code).entity(json).build();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

//            JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
//                    .add("exceptionType", exception.getClass().getName())
//                    .add("code", code);
//
//            if (exception.getMessage() != null) {
//                entityBuilder.add("error", exception.getMessage());
//            }

//            return Response.status(code).entity(entityBuilder.build()).build();
        }
    }
}
