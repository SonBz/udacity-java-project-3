package com.sbz.security.service;

import com.sbz.image.ImageService;
import com.sbz.security.application.StatusListener;
import com.sbz.security.data.AlarmStatus;
import com.sbz.security.data.ArmingStatus;
import com.sbz.security.data.SecurityRepository;
import com.sbz.security.data.Sensor;
import com.sbz.security.data.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    SecurityRepository securityRepository;
    @Mock
    ImageService imageService;
    @InjectMocks
    SecurityService securityService;

    Sensor sensor;

    @BeforeEach
    void setUp() {
        sensor = new Sensor("SBZ", SensorType.WINDOW);
    }

    //1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeSensorActivationStatus_alarmAndSensor_returnPendingAlarm(ArmingStatus armingStatus) {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.PENDING_ALARM, argumentCaptor.getValue());

    }

    //2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeSensorActivationStatus_alarmAndSensor_returnAlarm(ArmingStatus armingStatus) {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }

    //3
    @Test
    void changeSensorActivationStatus_alarmAndSensor_returnNoAlarm() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);

        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.NO_ALARM, argumentCaptor.getValue());
    }

    //4
    @Test
    void changeSensorActivationStatus_notAffect() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //5
    @Test
    void changeSensorActivationStatus_active_returnalArm() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }


    //6
    @Test
    void changeSensorActivationStatus_inactive_notChange() {
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //7
    @Test
    void processImage_containsCatAndArmedHome_ReturnAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }

    //8
    @Test
    void processImage_containsCatAndArmedHome_ReturnNoAlarm() {
        lenient().when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        lenient().when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.changeSensorActivationStatus(sensor, false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //9
    @Test
    void setArmingStatus_systemDisarmed_returnNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.NO_ALARM, captor.getValue());
    }

    //10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setArmingStatus_returnAllInactiveSensor(ArmingStatus armingStatus) {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityService.getSensors()).thenReturn(Set.of(sensor));
        securityService.setArmingStatus(armingStatus);
        for (Sensor sensor : securityRepository.getSensors()) {
            assertFalse(sensor.getActive());
        }
    }

    //11
    @Test
    void setAlarmStatus_armedHome_CamaraShowsCat() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, atMost(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    @Test
    void addSensor_checkIfSensorAdded() {
        securityService.addSensor(sensor);
        verify(securityRepository).addSensor(sensor);
    }

    @Test
    void removeSensor_checkIfSensorRemoved() {
        securityService.removeSensor(sensor);
        verify(securityRepository).removeSensor(sensor);
    }

    @Test
    void addStatusListener_checkIfListener() {
        securityService.addStatusListener(mock(StatusListener.class));
    }

    @Test
    void removeStatusListener_checkIfListener() {
        securityService.removeStatusListener(mock(StatusListener.class));
    }


    @Test
    void setArmingStatus_armedHome_returnAlarm() {
        securityService.setCatDetect(true);
        lenient().when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.setArmingStatus(securityService.getArmingStatus());
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void changeSensorActivationStatus_armingStatusDisarmed() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void choichoi() {
        assertNotNull(StyleService.HEADING_FONT);
    }
}
