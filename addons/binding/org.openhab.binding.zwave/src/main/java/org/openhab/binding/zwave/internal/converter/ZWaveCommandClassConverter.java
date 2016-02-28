/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.converter;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.zwave.handler.ZWaveThingHandler.ZWaveThingChannel;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

/**
 * ZWaveCommandClassConverter class. Base class for all converters that convert between Z-Wave command classes and
 * openHAB channels.
 *
 * @author Chris Jackson
 */
public abstract class ZWaveCommandClassConverter {
    private static Map<CommandClass, Class<? extends ZWaveCommandClassConverter>> converterMap = null;

    private static Logger logger = LoggerFactory.getLogger(ZWaveCommandClassConverter.class);

    private static BigDecimal ONE_POINT_EIGHT = new BigDecimal("1.8");
    private static BigDecimal THIRTY_TWO = new BigDecimal("32");

    /**
     * Constructor. Creates a new instance of the {@link ZWaveCommandClassConverter} class.
     *
     */
    public ZWaveCommandClassConverter() {
        super();
    }

    /**
     * Execute refresh method. This method is called every time a binding item is refreshed and the corresponding node
     * should be sent a message.
     *
     * @param node the {@link ZWaveNode} that is bound to the item.
     * @param endpointId the endpoint id to send the message.
     */
    public List<SerialMessage> executeRefresh(ZWaveThingChannel channel, ZWaveNode node) {
        return new ArrayList<SerialMessage>();
    }

    /**
     * Handles an incoming {@link ZWaveCommandClassValueEvent}. Implement this message in derived classes to convert the
     * value and post an update on the openHAB bus.
     *
     * @param event the received {@link ZWaveCommandClassValueEvent}.
     * @return
     */
    public State handleEvent(ZWaveThingChannel channel, ZWaveCommandClassValueEvent event) {
        return null;
    }

    /**
     * Receives a command from openHAB and translates it to an operation on the Z-Wave network.
     *
     * @param command the received command
     * @param node the {@link ZWaveNode} to send the command to
     * @param commandClass the {@link ZWaveCommandClass} to send the command to.
     * @param endpointId the endpoint ID to send the command to.
     */
    public List<SerialMessage> receiveCommand(ZWaveThingChannel channel, ZWaveNode node, Command command) {
        return new ArrayList<SerialMessage>();
    }

    public int getRefreshInterval() {
        return 0;
    }

    public static ZWaveCommandClassConverter getConverter(CommandClass commandClass) {
        if (converterMap == null) {
            converterMap = new HashMap<CommandClass, Class<? extends ZWaveCommandClassConverter>>();

            converterMap.put(CommandClass.ALARM, ZWaveAlarmConverter.class);
            converterMap.put(CommandClass.BASIC, ZWaveBasicConverter.class);
            converterMap.put(CommandClass.BATTERY, ZWaveBatteryConverter.class);
            converterMap.put(CommandClass.COLOR, ZWaveColorConverter.class);
            converterMap.put(CommandClass.CONFIGURATION, ZWaveConfigurationConverter.class);
            converterMap.put(CommandClass.METER, ZWaveMeterConverter.class);
            converterMap.put(CommandClass.SENSOR_ALARM, ZWaveAlarmSensorConverter.class);
            converterMap.put(CommandClass.SENSOR_BINARY, ZWaveBinarySensorConverter.class);
            converterMap.put(CommandClass.SENSOR_MULTILEVEL, ZWaveMultiLevelSensorConverter.class);
            converterMap.put(CommandClass.SWITCH_BINARY, ZWaveBinarySwitchConverter.class);
            converterMap.put(CommandClass.SWITCH_MULTILEVEL, ZWaveMultiLevelSwitchConverter.class);
            converterMap.put(CommandClass.THERMOSTAT_FAN_MODE, ZWaveThermostatFanModeConverter.class);
            converterMap.put(CommandClass.THERMOSTAT_FAN_STATE, ZWaveThermostatFanStateConverter.class);
            converterMap.put(CommandClass.THERMOSTAT_MODE, ZWaveThermostatModeConverter.class);
            converterMap.put(CommandClass.THERMOSTAT_OPERATING_STATE, ZWaveThermostatOperatingStateConverter.class);
            converterMap.put(CommandClass.THERMOSTAT_SETPOINT, ZWaveThermostatSetpointConverter.class);
        }

        Constructor<? extends ZWaveCommandClassConverter> constructor;
        try {
            if (converterMap.get(commandClass) == null) {
                logger.warn("CommandClass converter {} is not implemented!", commandClass.getLabel());
                return null;
            }
            constructor = converterMap.get(commandClass).getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            logger.error("Error getting converter {}", e.getMessage());
            return null;
        }
    }

    // ---------------------------------------
    // Common Conversions...

    /**
     * Convert temperature scales
     *
     * @param fromScale scale to convert FROM (0=C, 1=F)
     * @param toScale scale to convert TO (0=C, 1=F)
     * @param val value to convert
     * @return converted value
     */
    protected BigDecimal convertTemperature(int fromScale, int toScale, BigDecimal val) {
        BigDecimal valConverted;

        // For temperature, there are only two scales, so we simplify the conversion
        if (fromScale == 0 && toScale == 1) {
            // Scale is celsius, convert to fahrenheit
            valConverted = val.multiply(ONE_POINT_EIGHT).add(THIRTY_TWO).movePointRight(1).setScale(1,
                    RoundingMode.HALF_DOWN);
        } else if (fromScale == 1 && toScale == 0) {
            // Scale is fahrenheit, convert to celsius
            valConverted = val.subtract(THIRTY_TWO).divide(ONE_POINT_EIGHT, MathContext.DECIMAL32).setScale(1,
                    RoundingMode.HALF_DOWN);
        } else {
            valConverted = val;
        }

        logger.debug("Converted temperature from {}{} to {}{}", val, fromScale == 0 ? "C" : "F", valConverted,
                toScale == 0 ? "C" : "F");
        return valConverted;
    }

}
