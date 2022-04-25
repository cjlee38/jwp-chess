package chess.database.dao;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import chess.database.dao.spring.SpringBoardDao;
import chess.database.dao.spring.SpringGameDao;
import chess.database.dto.BoardDto;
import chess.database.dto.GameStateDto;
import chess.database.dto.PointDto;
import chess.database.dto.RouteDto;
import chess.database.dao.vanilla.JdbcBoardDao;
import chess.database.dao.vanilla.JdbcConnector;
import chess.database.dao.vanilla.JdbcGameDao;
import chess.domain.board.Board;
import chess.domain.board.InitialBoardGenerator;
import chess.domain.board.Point;
import chess.domain.board.Route;
import chess.domain.game.GameState;
import chess.domain.game.Ready;

@SpringBootTest
class BoardDaoTest {

    private static final String TEST_ROOM_NAME = "TESTING";
    private static final String TEST_CREATION_ROOM_NAME = "TESTING22";
    private static GameState state;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private GameDao gameDao;
    private BoardDao dao;

    @BeforeEach
    void setUp() {
        gameDao = new SpringGameDao(jdbcTemplate);
        state = new Ready();
        gameDao.saveGame(GameStateDto.of(state), TEST_ROOM_NAME);
        gameDao.saveGame(GameStateDto.of(state), TEST_CREATION_ROOM_NAME);

        dao = new SpringBoardDao(jdbcTemplate);
        Board board = Board.of(new InitialBoardGenerator());
        dao.saveBoard(BoardDto.of(board.getPointPieces()), TEST_ROOM_NAME);
    }

    @Test
    @DisplayName("말의 위치와 종류를 저장한다.")
    public void insert() {
        // given & when
        Board board = Board.of(new InitialBoardGenerator());
        // then
        assertThatCode(() -> dao.saveBoard(BoardDto.of(board.getPointPieces()), TEST_CREATION_ROOM_NAME))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("말의 위치와 종류를 조회한다.")
    public void select() {
        // given
        String roomName = TEST_ROOM_NAME;
        // when
        BoardDto boardDto = dao.readBoard(roomName);
        // then
        assertThat(boardDto.getPointPieces().size()).isEqualTo(32);
    }

    @Test
    @DisplayName("존재하지 않는 방을 조회하면 예외를 던진다.")
    public void selectMissingName() {
        // given & when
        String name = "missing";
        // then
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dao.readBoard(name));
    }

    @Test
    @DisplayName("말의 위치를 움직인다.")
    public void update() {
        // given & when
        String roomName = TEST_ROOM_NAME;
        Route route = Route.of(List.of("a2", "a4"));
        // then
        assertThatCode(() -> dao.updatePiece(RouteDto.of(route), roomName))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("말을 삭제한다.")
    public void delete() {
        // given & when
        String roomName = TEST_ROOM_NAME;
        Point point = Point.of("b2");
        // then
        assertThatCode(() -> dao.deletePiece(PointDto.of(point), roomName))
            .doesNotThrowAnyException();
    }

    @AfterEach
    void setDown() {
        dao.removeBoard(TEST_ROOM_NAME);
        dao.removeBoard(TEST_CREATION_ROOM_NAME);
        gameDao.removeGame(TEST_ROOM_NAME);
        gameDao.removeGame(TEST_CREATION_ROOM_NAME);
    }
}