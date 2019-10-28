package com.example.geoinformer.service;

import com.example.geoinformer.entity.Position;
import com.example.geoinformer.repository.PositionRepository;
import com.example.geoinformer.utility.LoggerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class PositionServiceImpl implements PositionService {

    private final Logger logger = LoggerFactory.getLogger(PositionServiceImpl.class.getName());

    // TODO Replace email to header changes
    private final String URL_FORMAT = "https://nominatim.openstreetmap.org/reverse?" +
            "email=okuziura@gmail.com&format=jsonv2&accept-language=en&zoom=18&lat=%f&lon=%f";

    @Autowired
    private PositionRepository positionRepository;

    /**
     * Метод, возвращающий по координатам в формате WGS 84 объект найденного
     * места в формате JSON.
     *
     * @param latitude  широта, указанная в градусах (в пределах от -90 до 90)
     * @param longitude долгота, указанная в градусах (в пределах от -180
     *        до 180)
     * @return ответ от сервера OpenStreetMap, включающий объект найденного
     *         места в формате JSON
     */
    @Override
    public ResponseEntity<Position> receivePosition(float latitude, float longitude) {
        if (latitude >= -90F && latitude <= 90F && longitude >= -180F && longitude <= 180F) {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(URL_FORMAT, latitude, longitude);
            logger.info(url);
            Position position = restTemplate.getForObject(url, Position.class);
            if (position != null) {
                if (
                        position.getOsmType() == null &&
                        position.getOsmId() == null &&
                        position.getLatitude() == 0 &&
                        position.getLongitude() == 0 &&
                        position.getCountry() == null &&
                        position.getType() == null &&
                        position.getName() == null
                ) {
                    logger.warn(LoggerMessage.SOURCE_NOMINATIM.getText() + "\n" + LoggerMessage.REPLY_NULL.getText());
                    logger.info(LoggerMessage.SOURCE_NOMINATIM.getText() + LoggerMessage.STATUS_SUCCESS.getText());
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
                } else {
                    logger.info(LoggerMessage.SOURCE_NOMINATIM.getText() + "\n" + position.toString());
                    logger.info(LoggerMessage.SOURCE_NOMINATIM.getText() + LoggerMessage.STATUS_SUCCESS.getText());
                    return new ResponseEntity<>(position, HttpStatus.OK); // 200
                }
            } else {
                logger.error(LoggerMessage.SOURCE_NOMINATIM.getText() + LoggerMessage.REPLY_NULL.getText());
                logger.info(LoggerMessage.SOURCE_NOMINATIM.getText() + LoggerMessage.STATUS_FAILED.getText());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500
            }
        } else {
            logger.warn(LoggerMessage.SOURCE_NOMINATIM.getText() + LoggerMessage.REPLY_BAD_INPUT.getText());
            logger.info(LoggerMessage.SOURCE_NOMINATIM.getText() + LoggerMessage.STATUS_FAILED.getText());
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY); // 422
        }
    }

    /**
     * Метод, сохраняющий объект места в БД и возвращающий его сущность из БД.
     * Если в БД ранее уже была создана запись, соответствующая этому месту,
     * то сохранение не осуществляется, а сущность возвращается на основе
     * старой записи в БД.
     *
     * @param positionFromExternalSource объект места, которое необходимо
     *        сохранить в БД
     * @return ответ от сервера, включающий сущность места в формате JSON,
     *         взятую из БД и соответствующую объекту места, переданного
     *         в параметре <tt>positionFromExternalSource</tt>
     */
    @Override
    public ResponseEntity<Position> savePosition(Position positionFromExternalSource) {
        if (positionFromExternalSource != null) {
            Position positionFromDB = positionRepository.findByLatitudeAndLongitude(
                    positionFromExternalSource.getLatitude(),
                    positionFromExternalSource.getLongitude()
            );
            if (positionFromDB == null) {
                positionFromDB = positionRepository.save(positionFromExternalSource);
                if (positionFromDB != null) {
                    logger.info(LoggerMessage.SOURCE_DATABASE.getText() + "\n" + positionFromDB.toString());
                    logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_SUCCESS.getText());
                    return new ResponseEntity<>(positionFromDB, HttpStatus.OK); // 200
                } else {
                    logger.error(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.REPLY_NULL.getText());
                    logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_FAILED.getText());
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500
                }
            } else {
                logger.info(LoggerMessage.SOURCE_DATABASE.getText() + "\n" + positionFromDB.toString());
                logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_SUCCESS.getText());
                return new ResponseEntity<>(positionFromDB, HttpStatus.OK); // 200
            }
        } else {
            logger.warn(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.REPLY_NULL.getText());
            logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_FAILED);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY); // 422
        }
    }

    /**
     * Метод, который возвращает все найденные в БД места из указанной страны.
     *
     * @param country название страны
     * @return список всех мест, найденных в БД и относящихся к указанной
     *         в параметре <tt>country</tt> стране
     */
    @Override
    public ResponseEntity<List<Position>> findPositionsByCountry(String country) {
        if(country != null) {
            country = country.toLowerCase();
            if(!country.equals("null")) {
                if (country.length() == 2) {
                    logger.info(LoggerMessage.SOURCE_DATABASE.getText());
                    List<Position> positions = positionRepository.findByCountryOrderByNameAsc(country);
                    for (Position position : positions) {
                        logger.info(position.overview());
                    }
                    logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_SUCCESS.getText());
                    return new ResponseEntity<>(positions, HttpStatus.OK); // 200
                } else {
                    logger.warn(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.REPLY_BAD_INPUT.getText());
                    logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_FAILED.getText());
                    return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY); // 422
                }
            } else {
                logger.info(LoggerMessage.SOURCE_DATABASE.getText());
                List<Position> positions = positionRepository.findByCountryOrderByNameAsc(null);
                for (Position position : positions) {
                    logger.info(position.overview());
                }
                logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_SUCCESS.getText());
                return new ResponseEntity<>(positions, HttpStatus.OK); // 200
            }
        } else {
            logger.info(LoggerMessage.SOURCE_DATABASE.getText());
            List<Position> positions = positionRepository.findByCountryOrderByNameAsc(null);
            for (Position position : positions) {
                logger.info(position.overview());
            }
            logger.info(LoggerMessage.SOURCE_DATABASE.getText() + LoggerMessage.STATUS_SUCCESS.getText());
            return new ResponseEntity<>(positions, HttpStatus.OK); // 200
        }
    }

//    @Override
//    public Position findPositionByName(String name) {
//        return positionRepository.findFirstByNameOrderById(name);
//    }
//
//    @Override
//    public Position findPositionByCoords(float latitude, float longitude) {
//        return positionRepository.findByLatitudeAndLongitude(latitude, longitude);
//    }
//
//    @Override
//    public void updateAllPositions() {
//        List<Position> positions = positionRepository.findAll();
//        Position positionNew = null;
//        String url = null;
//        float latitude;
//        float longitude;
//        RestTemplate restTemplate = new RestTemplate();
//        for (Position position : positions) {
//            latitude = position.getLatitude();
//            longitude = position.getLongitude();
//            url = String.format(URL_FORMAT, latitude, longitude);
//            positionNew = restTemplate.getForObject(url, Position.class);
//            position.setLatitude(positionNew.getLatitude());
//            position.setLongitude(positionNew.getLongitude());
//            position.setCountry(positionNew.getCountry());
//            position.setType(positionNew.getType());
//            position.setName(positionNew.getName());
//            position.setOsmId(positionNew.getOsmId());
//            position.setOsmType(positionNew.getOsmType());
//            positionRepository.save(position);
//        }
//    }
}
