/*
 * Copyright 2016 Naver Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.common.server.bo.codec.stat.v2;

import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.server.bo.codec.stat.AgentStatCodec;
import com.navercorp.pinpoint.common.server.bo.codec.stat.AgentStatDataPointCodec;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.AgentStatHeaderDecoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.AgentStatHeaderEncoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.BitCountingHeaderEncoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.StrategyAnalyzer;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.UnsignedIntegerEncodingStrategy;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.UnsignedShortEncodingStrategy;
import com.navercorp.pinpoint.common.server.bo.codec.strategy.EncodingStrategy;
import com.navercorp.pinpoint.common.server.bo.stat.ActiveTraceBo;
import com.navercorp.pinpoint.common.trace.SlotType;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author HyunGil Jeong
 */
@Component("activeTraceCodecV2")
public class ActiveTraceCodecV2 extends AbstractAgentStatCodecV2<ActiveTraceBo> {

    @Autowired
    public ActiveTraceCodecV2(AgentStatDataPointCodec codec) {
        super(codec);
    }

    @Override
    protected CodecEncoder createCodecEncoder() {
        return new ActiveTraceCodecEncoder(codec);
    }

    @Override
    protected CodecDecoder createCodecDecoder() {
        return new ActiveTraceCodecDecoder(codec);
    }

    public static class ActiveTraceCodecEncoder implements AgentStatCodec.CodecEncoder<ActiveTraceBo> {

        private final AgentStatDataPointCodec codec;
        private final UnsignedShortEncodingStrategy.Analyzer.Builder versionAnalyzerBuilder = new UnsignedShortEncodingStrategy.Analyzer.Builder();
        private final UnsignedIntegerEncodingStrategy.Analyzer.Builder schemaTypeAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        private final UnsignedIntegerEncodingStrategy.Analyzer.Builder fastTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        private final UnsignedIntegerEncodingStrategy.Analyzer.Builder normalTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        private final UnsignedIntegerEncodingStrategy.Analyzer.Builder slowTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        private final UnsignedIntegerEncodingStrategy.Analyzer.Builder verySlowTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();

        public ActiveTraceCodecEncoder(AgentStatDataPointCodec codec) {
            this.codec = codec;
        }

        @Override
        public void addValue(ActiveTraceBo activeTraceBo) {
            versionAnalyzerBuilder.addValue(activeTraceBo.getVersion());
            schemaTypeAnalyzerBuilder.addValue(activeTraceBo.getHistogramSchemaType());
            final Map<SlotType, Integer> activeTraceCounts = activeTraceBo.getActiveTraceCounts();
            fastTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.FAST, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
            normalTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.NORMAL, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
            slowTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.SLOW, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
            verySlowTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.VERY_SLOW, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
        }

        @Override
        public void encode(AgentStatDataPointCodec codec, Buffer valueBuffer) {
            StrategyAnalyzer<Short> versionStrategyAnalyzer = versionAnalyzerBuilder.build();
            StrategyAnalyzer<Integer> schemaTypeStrategyAnalyzer = schemaTypeAnalyzerBuilder.build();
            StrategyAnalyzer<Integer> fastTraceCountsStrategyAnalyzer = fastTraceCountsAnalyzerBuilder.build();
            StrategyAnalyzer<Integer> normalTraceCountsStrategyAnalyzer = normalTraceCountsAnalyzerBuilder.build();
            StrategyAnalyzer<Integer> slowTraceCountsStrategyAnalyzer = slowTraceCountsAnalyzerBuilder.build();
            StrategyAnalyzer<Integer> verySlowTraceCountsStrategyAnalyzer = verySlowTraceCountsAnalyzerBuilder.build();

            // encode header
            AgentStatHeaderEncoder headerEncoder = new BitCountingHeaderEncoder();
            headerEncoder.addCode(versionStrategyAnalyzer.getBestStrategy().getCode());
            headerEncoder.addCode(schemaTypeStrategyAnalyzer.getBestStrategy().getCode());
            headerEncoder.addCode(fastTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
            headerEncoder.addCode(normalTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
            headerEncoder.addCode(slowTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
            headerEncoder.addCode(verySlowTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
            final byte[] header = headerEncoder.getHeader();
            valueBuffer.putPrefixedBytes(header);
            // encode values
            this.codec.encodeValues(valueBuffer, versionStrategyAnalyzer.getBestStrategy(), versionStrategyAnalyzer.getValues());
            this.codec.encodeValues(valueBuffer, schemaTypeStrategyAnalyzer.getBestStrategy(), schemaTypeStrategyAnalyzer.getValues());
            this.codec.encodeValues(valueBuffer, fastTraceCountsStrategyAnalyzer.getBestStrategy(), fastTraceCountsStrategyAnalyzer.getValues());
            this.codec.encodeValues(valueBuffer, normalTraceCountsStrategyAnalyzer.getBestStrategy(), normalTraceCountsStrategyAnalyzer.getValues());
            this.codec.encodeValues(valueBuffer, slowTraceCountsStrategyAnalyzer.getBestStrategy(), slowTraceCountsStrategyAnalyzer.getValues());
            this.codec.encodeValues(valueBuffer, verySlowTraceCountsStrategyAnalyzer.getBestStrategy(), verySlowTraceCountsStrategyAnalyzer.getValues());
        }

    }

    public static class ActiveTraceCodecDecoder implements AgentStatCodec.CodecDecoder<ActiveTraceBo> {

        private final AgentStatDataPointCodec codec;
        private List<Short> versions;
        private List<Integer> schemaTypes;
        private List<Integer> fastTraceCounts;
        private List<Integer> normalTraceCounts;
        private List<Integer> slowTraceCounts;
        private List<Integer> verySlowTraceCounts;

        public ActiveTraceCodecDecoder(AgentStatDataPointCodec codec) {
            this.codec = codec;
        }

        @Override
        public void decode(Buffer valueBuffer, AgentStatHeaderDecoder headerDecoder, int valueSize) {
            EncodingStrategy<Short> versionEncodingStrategy = UnsignedShortEncodingStrategy.getFromCode(headerDecoder.getCode());
            EncodingStrategy<Integer> schemaTypeEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
            EncodingStrategy<Integer> fastTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
            EncodingStrategy<Integer> normalTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
            EncodingStrategy<Integer> slowTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
            EncodingStrategy<Integer> verySlowTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
            // decode values
            this.versions = this.codec.decodeValues(valueBuffer, versionEncodingStrategy, valueSize);
            this.schemaTypes = this.codec.decodeValues(valueBuffer, schemaTypeEncodingStrategy, valueSize);
            this.fastTraceCounts = this.codec.decodeValues(valueBuffer, fastTraceCountsEncodingStrategy, valueSize);
            this.normalTraceCounts = this.codec.decodeValues(valueBuffer, normalTraceCountsEncodingStrategy, valueSize);
            this.slowTraceCounts = this.codec.decodeValues(valueBuffer, slowTraceCountsEncodingStrategy, valueSize);
            this.verySlowTraceCounts = this.codec.decodeValues(valueBuffer, verySlowTraceCountsEncodingStrategy, valueSize);
        }

        @Override
        public ActiveTraceBo getValue(int index) {
            ActiveTraceBo activeTraceBo = new ActiveTraceBo();
            activeTraceBo.setVersion(versions.get(index));
            activeTraceBo.setHistogramSchemaType(schemaTypes.get(index));
            Map<SlotType, Integer> activeTraceCounts = new HashMap<SlotType, Integer>();
            activeTraceCounts.put(SlotType.FAST, fastTraceCounts.get(index));
            activeTraceCounts.put(SlotType.NORMAL, normalTraceCounts.get(index));
            activeTraceCounts.put(SlotType.SLOW, slowTraceCounts.get(index));
            activeTraceCounts.put(SlotType.VERY_SLOW, verySlowTraceCounts.get(index));
            activeTraceBo.setActiveTraceCounts(activeTraceCounts);
            return activeTraceBo;
        }

    }

}
