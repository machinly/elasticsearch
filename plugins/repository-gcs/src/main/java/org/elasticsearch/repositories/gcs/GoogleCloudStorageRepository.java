/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.repositories.gcs;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.repositories.RepositoryException;
import org.elasticsearch.repositories.blobstore.BlobStoreRepository;

import java.util.function.Function;

import static org.elasticsearch.common.settings.Setting.Property;
import static org.elasticsearch.common.settings.Setting.boolSetting;
import static org.elasticsearch.common.settings.Setting.byteSizeSetting;
import static org.elasticsearch.common.settings.Setting.simpleString;
import static org.elasticsearch.common.settings.Setting.timeSetting;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;

class GoogleCloudStorageRepository extends BlobStoreRepository {

    private final Logger logger = LogManager.getLogger(GoogleCloudStorageRepository.class);
    private final DeprecationLogger deprecationLogger = new DeprecationLogger(logger);

    // package private for testing
    static final ByteSizeValue MIN_CHUNK_SIZE = new ByteSizeValue(1, ByteSizeUnit.BYTES);
    static final ByteSizeValue MAX_CHUNK_SIZE = new ByteSizeValue(100, ByteSizeUnit.MB);

    static final String TYPE = "gcs";

    static final TimeValue NO_TIMEOUT = timeValueMillis(-1);

    static final Setting<String> BUCKET =
            simpleString("bucket", Property.NodeScope, Property.Dynamic);
    static final Setting<String> BASE_PATH =
            simpleString("base_path", Property.NodeScope, Property.Dynamic);
    static final Setting<Boolean> COMPRESS =
            boolSetting("compress", false, Property.NodeScope, Property.Dynamic);
    static final Setting<ByteSizeValue> CHUNK_SIZE =
            byteSizeSetting("chunk_size", MAX_CHUNK_SIZE, MIN_CHUNK_SIZE, MAX_CHUNK_SIZE, Property.NodeScope, Property.Dynamic);
    static final Setting<String> CLIENT_NAME = new Setting<>("client", "default", Function.identity());

    @Deprecated
    static final Setting<String> APPLICATION_NAME =
        new Setting<>("application_name", "", Function.identity(), Property.NodeScope, Property.Dynamic);

    @Deprecated
    static final Setting<TimeValue> HTTP_READ_TIMEOUT =
            timeSetting("http.read_timeout", NO_TIMEOUT, Property.NodeScope, Property.Dynamic);

    @Deprecated
    static final Setting<TimeValue> HTTP_CONNECT_TIMEOUT =
            timeSetting("http.connect_timeout", NO_TIMEOUT, Property.NodeScope, Property.Dynamic);

    private final GoogleCloudStorageService storageService;
    private final BlobPath basePath;
    private final boolean compress;
    private final ByteSizeValue chunkSize;
    private final String bucket;
    private final String clientName;

    GoogleCloudStorageRepository(RepositoryMetaData metadata, Environment environment,
                                        NamedXContentRegistry namedXContentRegistry,
                                        GoogleCloudStorageService storageService) {
        super(metadata, environment.settings(), namedXContentRegistry);
        this.storageService = storageService;

        String basePath = BASE_PATH.get(metadata.settings());
        if (Strings.hasLength(basePath)) {
            BlobPath path = new BlobPath();
            for (String elem : basePath.split("/")) {
                path = path.add(elem);
            }
            this.basePath = path;
        } else {
            this.basePath = BlobPath.cleanPath();
        }

        this.compress = getSetting(COMPRESS, metadata);
        this.chunkSize = getSetting(CHUNK_SIZE, metadata);
        this.bucket = getSetting(BUCKET, metadata);
        this.clientName = CLIENT_NAME.get(metadata.settings());
        logger.debug("using bucket [{}], base_path [{}], chunk_size [{}], compress [{}]", bucket, basePath, chunkSize, compress);

        final String applicationName = APPLICATION_NAME.get(metadata.settings());
        if (Strings.hasText(applicationName)) {
            deprecationLogger.deprecated("Setting [application_name] in repository settings is deprecated, " +
                "it must be specified in the client settings instead");
            storageService.setOverrideApplicationName(applicationName);
        }

        final TimeValue connectTimeout = HTTP_CONNECT_TIMEOUT.get(metadata.settings());
        if ((connectTimeout != null) && (connectTimeout.millis() != NO_TIMEOUT.millis())) {
            deprecationLogger.deprecated("Setting [http.connect_timeout] in repository settings is deprecated, " +
                "it must be specified in the client settings instead");
            storageService.setOverrideConnectTimeout(connectTimeout);
        }

        final TimeValue readTimeout = HTTP_READ_TIMEOUT.get(metadata.settings());
        if ((readTimeout != null) && (readTimeout.millis() != NO_TIMEOUT.millis())) {
            deprecationLogger.deprecated("Setting [http.read_timeout] in repository settings is deprecated, " +
                "it must be specified in the client settings instead");
            storageService.setOverrideReadTimeout(readTimeout);
        }
    }

    @Override
    protected GoogleCloudStorageBlobStore createBlobStore() {
        return new GoogleCloudStorageBlobStore(settings, bucket, clientName, storageService);
    }

    @Override
    protected BlobPath basePath() {
        return basePath;
    }

    @Override
    protected boolean isCompress() {
        return compress;
    }

    @Override
    protected ByteSizeValue chunkSize() {
        return chunkSize;
    }

    /**
     * Get a given setting from the repository settings, throwing a {@link RepositoryException} if the setting does not exist or is empty.
     */
    static <T> T getSetting(Setting<T> setting, RepositoryMetaData metadata) {
        T value = setting.get(metadata.settings());
        if (value == null) {
            throw new RepositoryException(metadata.name(), "Setting [" + setting.getKey() + "] is not defined for repository");
        }
        if ((value instanceof String) && (Strings.hasText((String) value)) == false) {
            throw new RepositoryException(metadata.name(), "Setting [" + setting.getKey() + "] is empty for repository");
        }
        return value;
    }
}
