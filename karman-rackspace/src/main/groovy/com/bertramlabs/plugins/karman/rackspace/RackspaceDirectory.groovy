package com.bertramlabs.plugins.karman.rackspace

import com.bertramlabs.plugins.karman.CloudFile
import com.bertramlabs.plugins.karman.Directory
import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicHeader
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.apache.http.util.EntityUtils

/**
 * Created by davidestes on 10/12/15.
 */
class RackspaceDirectory extends Directory {


	/**
	 * Check if bucket exists
	 * @return Boolean
	 */
	Boolean exists() {
	}

	/**
	 * List bucket files
	 * @param options (prefix, marker, delimiter and maxKeys)
	 * @return List
	 */
	List listFiles(options = [:]) {
		RackspaceStorageProvider rackspaceProvider = (RackspaceStorageProvider) provider
		URI listUri
		URIBuilder uriBuilder = new URIBuilder("${rackspaceProvider.getEndpointUrl()}/${name}".toString())

		options?.each { entry ->
			uriBuilder.addParameter(entry.key,entry.value)
		}

		listUri = uriBuilder.build()

		HttpGet request = new HttpGet(listUri)

		request.addHeader("Accept", "application/json")
		request.addHeader(new BasicHeader('X-Auth-Token', rackspaceProvider.getToken()))
		HttpClient client = new DefaultHttpClient()
		HttpParams params = client.getParams()
		HttpConnectionParams.setConnectionTimeout(params, 30000)
		HttpConnectionParams.setSoTimeout(params, 20000)
		HttpResponse response = client.execute(request)

		HttpEntity responseEntity = response.getEntity()
		def jsonBody = new JsonSlurper().parse(new InputStreamReader(responseEntity.content))
		return jsonBody?.collect { meta ->
			cloudFileFromRackspaceMeta(meta)
		}
		EntityUtils.consume(responseEntity)
	}

	/**
	 * Create bucket for a given region (default to region in config if not defined)
	 * @return Bucket
	 */
	def save() {RackspaceStorageProvider rackspaceProvider = (RackspaceStorageProvider) provider
		URIBuilder uriBuilder = new URIBuilder("${rackspaceProvider.getEndpointUrl()}/${name}".toString())
		HttpPut request = new HttpPut(uriBuilder.build())
		request.addHeader("Accept", "application/json")
		request.addHeader(new BasicHeader('X-Auth-Token', rackspaceProvider.getToken()))

		HttpClient client = new DefaultHttpClient()
		HttpParams params = client.getParams()
		HttpConnectionParams.setConnectionTimeout(params, 30000)
		HttpConnectionParams.setSoTimeout(params, 20000)
		HttpResponse response = client.execute(request)
		EntityUtils.consume(response.entity)
		if(response.statusLine.statusCode == 200) {

			return true
		} else {
			return false
		}
	}

	CloudFile getFile(String name) {
		new RackspaceCloudFile(
			provider: provider,
			parent: this,
			name: name
		)
	}

	// PRIVATE

	private RackspaceCloudFile cloudFileFromRackspaceMeta(Map meta) {
		new RackspaceCloudFile(
			provider: provider,
			parent: this,
			name: meta.name
		)
	}
}