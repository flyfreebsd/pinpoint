/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.handler;

import com.navercorp.pinpoint.collector.dao.SqlMetaDataDao;
import com.navercorp.pinpoint.io.request.ServerRequest;
import com.navercorp.pinpoint.io.request.ServerResponse;
import com.navercorp.pinpoint.thrift.dto.TResult;
import com.navercorp.pinpoint.thrift.dto.TSqlMetaData;
import org.apache.thrift.TBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author emeroad
 */
@Service
public class SqlMetaDataHandler implements RequestResponseHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SqlMetaDataDao sqlMetaDataDao;

    @Autowired
    public SqlMetaDataHandler(SqlMetaDataDao sqlMetaDataDao) {
        this.sqlMetaDataDao = sqlMetaDataDao;
    }

    @Override
    public void handleRequest(ServerRequest serverRequest, ServerResponse serverResponse) {
        final Object data = serverRequest.getData();
        if (data instanceof TBase<?, ?>) {
            TBase<?, ?> tBase = handleRequest((TBase<?, ?>) data);
            serverResponse.write(tBase);
            return;
        }

        logger.warn("invalid serverRequest:{}", serverRequest);
    }

    private TBase<?, ?> handleRequest(TBase<?, ?> tbase) {
        if (!(tbase instanceof TSqlMetaData)) {
            logger.error("invalid tbase:{}", tbase);
            return null;
        }

        final TSqlMetaData sqlMetaData = (TSqlMetaData) tbase;

        if (logger.isDebugEnabled()) {
            logger.debug("Received SqlMetaData:{}", sqlMetaData);
        }

        try {
            sqlMetaDataDao.insert(sqlMetaData);
        } catch (Exception e) {
            logger.warn("{} handler error. Caused:{}", this.getClass(), e.getMessage(), e);
            TResult result = new TResult(false);
            result.setMessage(e.getMessage());
            return result;
        }
        return new TResult(true);
    }

}
