package com.azure.kusto;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.http.ProxyOptions;
import com.azure.identity.AzureCliCredentialBuilder;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientFactory;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import com.microsoft.azure.kusto.data.http.HttpClientProperties;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;


/**
 * A simple archetype created for demo of the Proxy config that can be set which can bypass the calls to a specific URL pattern
 * A proxt server is setup to block requests to a specific URL pattern, in this case, this pattern is the Kusto URL.
 */
public class App 
{
    public static void main( String[] args )
    {
        if(args.length < 3) {
            System.out.println("Usage: App <clusterUrl> <tenant> <databaseName>");
            return;
        }

        App proxyApp = new App();
        InetSocketAddress proxyAddress = proxyApp.setupProxy(9090, "kusto.windows.net");
        try {
            long oneDay = proxyApp.queryKusto(args[0], args[1], args[2], proxyAddress);
            System.out.println("==========================================================================");
            System.out.println("One day in seconds: " + oneDay);
            System.out.println("==========================================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Long queryKusto(String cluster, String tenantId , String database, InetSocketAddress proxyInet) throws Exception
    {

        String aFunQuery = " let toUnixTime = (dt:datetime) { (dt - datetime(1970-01-01)) / 1s };print elapsedTime=toUnixTime(datetime('1970-01-02'))";

        TokenRequestContext tokenRequestContext = new TokenRequestContext().setScopes(Collections.singletonList(String.format("%s/.default", cluster))).setTenantId(tenantId);
        AccessToken azCliToken = new AzureCliCredentialBuilder().build().getToken(tokenRequestContext).block();
        
        ProxyOptions noKustoLittleProxy = new ProxyOptions(ProxyOptions.Type.HTTP, proxyInet);
        // Do not use this for Kusto
        String byPassHost = new URI(cluster).getHost();
        System.out.println("Bypassing host: " + byPassHost);
        noKustoLittleProxy.setNonProxyHosts(byPassHost);


        HttpClientProperties propertiesBuilder = HttpClientProperties.builder().proxy(noKustoLittleProxy).build();
        
        
        ConnectionStringBuilder csb = ConnectionStringBuilder.createWithAadAccessTokenAuthentication(cluster, azCliToken.getToken());
        Client kustoClient = ClientFactory.createClient(csb, propertiesBuilder);

        KustoOperationResult kopsResult =  kustoClient.execute(database, aFunQuery);
        // there is one row for sure
        kopsResult.getPrimaryResults().next();

        return kopsResult.getPrimaryResults().getBigDecimal("elapsedTime").toBigInteger().longValue();
    }


    /**
     * Setup a proxy server that blocks requests to a specific URL pattern
     * @param proxyPort
     * @param urlPatternToBlock The URL pattern to block. The idea is to block requests to this URL pattern
     * @return A handle to where the proxy is running
     */
    public InetSocketAddress setupProxy(int proxyPort, final String urlPatternToBlock) {
        HttpProxyServer server =
            DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
        .withFiltersSource(new HttpFiltersSourceAdapter() {
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        if(originalRequest.uri().contains(urlPatternToBlock)) {
                            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, null);
                        }
                        return null;
                    }

                    @Override
                    public HttpObject serverToProxyResponse(HttpObject httpObject) {
                        return httpObject;
                    }
                };
            }
        })
        .start();
        return server.getListenAddress();
    }
}
//https://microsoftedge.github.io/Demos/json-dummy-data/64KB.json
