package katalon.codereuse

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.network.ProxyInformation

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.http.Header
import org.apache.http.HeaderElement
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.LaxRedirectStrategy
import org.junit.After
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.client.CredentialsProvider
import org.apache.http.auth.AuthScope

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Downloader drives HTTP request & response to download a distribute file from a web site.
 * Downloader is Proxy aware, but the caller need to pass the Proxy config to the constructor.
 * 
 * 
 * Copy&pasted from
 * https://gist.github.com/rponte/09ddc1aa7b9918b52029
 */
public class Downloader {

	private static final Logger logger_ = LoggerFactory.getLogger(Downloader.class)

	static String getVersion() {
		return Version.getVersion()
	}

	// Proxy config
	RequestConfig requestConfig

	Downloader() {
		requestConfig = null
		HttpHost proxy = Downloader.getProxy()
		if (proxy != null) {
			requestConfig = RequestConfig.custom().setProxy(proxy).build()
		}
	}

	/**
	 * 
	 * @param url
	 * @param credentials
	 * @return
	 */
	private static CloseableHttpClient makeHttpClient(URL url, Credentials credentials) {
		if (url == null) {
			throw new IllegalArgumentException("url is required")
		}
		CredentialsProvider provider = new BasicCredentialsProvider()
		provider.setCredentials(AuthScope.ANY, credentials);
		CloseableHttpClient httpclient = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCredentialsProvider(provider)
				.build()
		return httpclient
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public Header[] getAllHeaders(URL url, Credentials credentials) {
		CloseableHttpClient httpclient = makeHttpClient(url, credentials)
		try {
			HttpHead head = new HttpHead(url.toURI())
			if (this.requestConfig != null) {
				// set Proxy config if necessary
				head.setConfig(requestConfig)
			}
			HttpResponse response = httpclient.execute(head)
			Header[] headers = response.getAllHeaders()
			return headers
		} catch (Exception e) {
			throw new IllegalStateException(e)
		} finally {
			IOUtils.closeQuietly(httpclient)
		}
	}


	/**
	 * 
	 * @param url
	 * @param name
	 * @return
	 */
	public Header getHeader(Header[] headers, String headerName) {
		for (Header header : headers) {
			if (header.getName() == headerName) {
				return header
			}
		}
		return null
	}

	public String getHttpStatus(Header[] headers) {
		return this.getHeader(headers, 'Status')
	}

	/**
	 * Provided that the url reponds with a HTTP Header
	 *   Content-Disposition: attachment; filename=MyCustomKeywords-0.2.zip
	 * then returns a String
	 *   MyCustomKeywords-0.2.zip
	 * 
	 * @param url
	 * @return
	 */
	static Pattern ptn = Pattern.compile(/filename=([\S]+)$/)


	public String getContentDispositionFilename(Header[] headers) {
		def headerName = 'Content-Disposition'
		Header header = this.getHeader(headers, headerName)
		if (header != null) {
			HeaderElement[] elements = header.getElements()
			for (HeaderElement he : elements) {
				Matcher m = ptn.matcher(he.toString())
				if (m.find()) {
					return m.group(1)
				}
			}
			logger_.info("in ${header}, where filename=xxxx is not found")
		} else {
			logger_.info("in ${headers}, ${headerName} Header is not found")
		}
		return null
	}

	/**
	 * 
	 * @param url
	 * @param dstFile
	 * @return
	 */
	public File download(URL url, Credentials credentials, File distributedFile) {
		CloseableHttpClient httpclient = makeHttpClient(url, credentials)
		try {
			HttpGet get = new HttpGet(url.toURI())
			if (this.requestConfig != null) {
				// set Proxy config if necessary
				get.setConfig(requestConfig)
			}
			File downloaded = httpclient.execute(get,
					new FileDownloadResponseHandler(distributedFile))
			return downloaded
		} catch (Exception e) {
			throw new IllegalStateException(e)
		} finally {
			IOUtils.closeQuietly(httpclient)
		}
	}

	/* --------------------------------------------------------------------- */

	/**
	 * If your Katalon Studio is configured with MANUAL PROXY,
	 * then retrieve the proxy config to construct an instance of
	 * org.apache.http.HttpHost
	 * and return it. Otherwise return null.
	 * 
	 * @return
	 */
	static HttpHost getProxy() {
		ProxyInformation pi = RunConfiguration.getProxyInformation()
		String proxyOption = pi.getProxyOption()
		String proxyServerType = pi.getProxyServerType()
		String username = pi.getUsername()
		String password = pi.getPassword()
		String proxyServerAddress = pi.getProxyServerAddress()
		int proxyServerPort = pi.getProxyServerPort()
		if (proxyOption == 'MANUAL_CONFIG' && proxyServerType == 'HTTP') {
			HttpHost host = new HttpHost(
					proxyServerAddress, proxyServerPort,
					proxyServerType)
			logger_.debug("proxy host is '${host.toString()}'")
			return host
		}
		return null
	}

	/**
	 * 
	 * @author kazurayam
	 *
	 */
	static class FileDownloadResponseHandler implements ResponseHandler<File> {
		private final File target

		public FileDownloadResponseHandler(File target) {
			this.target = target
		}

		@Override
		public File handleResponse(HttpResponse response)
		throws ClientProtocolException, IOException {
			InputStream source = response.getEntity().getContent()
			FileUtils.copyInputStreamToFile(source, this.target)
			return this.target
		}
	}
}
