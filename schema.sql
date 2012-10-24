CREATE TABLE passwd (
	username VARCHAR(200) NOT NULL PRIMARY KEY,
	password VARCHAR(200) NOT NULL
	);

--FOREIGN KEY (director) REFERENCES passwd (username)
CREATE TABLE contest (
	contest_id INT AUTO_INCREMENT PRIMARY KEY,
	title     VARCHAR(200),
	subtitle  VARCHAR(200),
	logo      VARCHAR(200),
	shortname VARCHAR(20),
	director  VARCHAR(200),
	director_password VARCHAR(200),
	scoreboard        CHAR(1) NOT NULL DEFAULT 'Y',            -- Y/N
	scoreboard_images CHAR(1) NOT NULL DEFAULT 'Y',            -- Y/N
	scoreboard_popups CHAR(1) NOT NULL DEFAULT 'Y',            -- Y/N
	scoreboard_order  CHAR(1) NOT NULL DEFAULT 'n', -- s=score, o=ordinal,
	                                                -- n=name
	enabled           CHAR(1) NOT NULL DEFAULT 'N',            -- Y/N
	scoreboard_fanfare CHAR(1) NOT NULL DEFAULT 'Y',           -- Y/N
	teams_can_change_name CHAR(1) NOT NULL DEFAULT 'N',        -- Y/N
	teams_can_change_description CHAR(1) NOT NULL DEFAULT 'N', -- Y/N
	teams_can_change_password CHAR(1) NOT NULL DEFAULT 'Y',    -- Y/N
	score_system      CHAR(1) NOT NULL DEFAULT 'A', -- A=ACM, T=Topcoder
	collaboration     CHAR(1) NOT NULL DEFAULT 'N',            -- Y/N
	handout_html      TEXT,
	phase1_name       VARCHAR(20),
	phase1_ends       DATETIME,
	phase2_name       VARCHAR(20),
	phase2_ends       DATETIME,
	phase3_name       VARCHAR(20),
	phase3_ends       DATETIME,
	phase4_name       VARCHAR(20),
	phase4_ends       DATETIME
	);

--FOREIGN KEY (contest) REFERENCES contest (contest_id)
CREATE TABLE problem (
	contest           INT,
	problem_number    INT,
	problem_name      VARCHAR(200),
	spec_file         VARCHAR(200),
	spec_name         VARCHAR(200),
	solution_file     VARCHAR(200),
	solution_name     VARCHAR(200),
--	solution_visible  CHAR(1) NOT NULL DEFAULT 'N',            -- Y/N
--	challenge_phase   CHAR(1) NOT NULL DEFAULT 'N',            -- Y/N
--	visible           CHAR(1) NOT NULL DEFAULT 'Y',            -- Y/N
--	allow_submissions CHAR(1) NOT NULL DEFAULT 'Y',            -- Y/N
	judged_by         VARCHAR(255),
	scoreboard_solved_image VARCHAR(255),
	difficulty        INT,
	score_by_access_time CHAR(1) NOT NULL DEFAULT 'N',         -- Y/N
	start_time        DATETIME,
	allocated_minutes INT,
	runtime_limit     INT NOT NULL DEFAULT 50,
	pp_scoreboard     TINYINT NOT NULL,   --bit vector over contest phases
	pp_read_problem   TINYINT NOT NULL,
	pp_submit         TINYINT NOT NULL,
	pp_read_opponent  TINYINT NOT NULL,
	pp_challenge      TINYINT NOT NULL,
	pp_read_solution  TINYINT NOT NULL,
	PRIMARY KEY (contest, problem_number)
	);

--FOREIGN KEY (contest) REFERENCES contest (contest_id)
CREATE TABLE team (
	team_number INT AUTO_INCREMENT PRIMARY KEY,
	team_name   VARCHAR(200) NOT NULL,
	description VARCHAR(200),
	contest     INT NOT NULL,
	score       INT,
	score_alt   INT,
	user        VARCHAR(200),
	ordinal     INT NOT NULL,
	password    VARCHAR(200),
	last_refreshed DATETIME,
	last_message_acked INT,
	UNIQUE INDEX (contest, ordinal),
	UNIQUE INDEX (contest, user)
	);

--FOREIGN KEY (contest) REFERENCES contest (contest_id)
CREATE TABLE judge (
	judge_id       INT AUTO_INCREMENT PRIMARY KEY,
	contest        INT NOT NULL,
	judge_user     VARCHAR(200) NOT NULL,
	judge_password VARCHAR(200),
	last_refreshed DATETIME,
	last_message_acked INT,
	UNIQUE INDEX (contest, judge_user)
	);

--FOREIGN KEY (team_number)    REFERENCES team (team_number)
--FOREIGN KEY (problem_number) REFERENCES problem (problem_number)
CREATE TABLE results (
	team_number INTEGER NOT NULL,
	problem_number INTEGER NOT NULL,
	thetime               INTEGER,
	incorrect_submissions INTEGER,
	opened                DATETIME,
	score                 INTEGER,
	score_alt             INTEGER,
	source_file           VARCHAR(200),
	source_name           VARCHAR(200),
	PRIMARY KEY (team_number, problem_number)
	);

--FOREIGN KEY (team)    REFERENCES team (team_number)
--FOREIGN KEY (problem) REFERENCES problem (problem_number)
--FOREIGN KEY (judge)   REFERENCES judge (judge_id)
CREATE TABLE submission (
	id         INTEGER AUTO_INCREMENT PRIMARY KEY,
	team       INTEGER NOT NULL,
	problem    INTEGER NOT NULL,
	submitted  DATETIME NOT NULL,
	minutes    INTEGER NOT NULL,
	file       VARCHAR(200),
	given_name VARCHAR(200),
	status     VARCHAR(200) NOT NULL,
	judge      INT,
	coauthors  VARCHAR(200)
	);

--FOREIGN KEY (creator) REFERENCES team (team_number)
--FOREIGN KEY (submission) REFERENCES submission (id)
CREATE TABLE challenge (
	id         INTEGER AUTO_INCREMENT PRIMARY KEY,
	creator    INTEGER NOT NULL,
	submission INTEGER NOT NULL,
	input_file VARCHAR(200) NOT NULL,
	status     VARCHAR(200) NOT NULL,
	expected_file VARCHAR(200),
	output_file VARCHAR(200)
	);

CREATE TABLE worker (
	id                 VARCHAR(200) PRIMARY KEY,
	accepted_languages VARCHAR(200) NOT NULL,
	last_refreshed     DATETIME NOT NULL,
	description VARCHAR(1000),
	status      VARCHAR(1000)
	);

--FOREIGN KEY (owner) REFERENCES worker (id)
--type is one of:
--  'S' - system test of a submission
--  'E' - generate expected output of a system test
--  'U' - user test
--  'V' - validate an input file
--  'C' - generate expected output of a challenge
--  'D' - generate actual output of a challenge
--if type='S' then
--   there should be a test_result record with job=id
--if type='U' then
--   user_uid must not be null
--if type='V' or type='C' then
--   callback_data should be "challenge %d" where %d is the challenge id
--
--result_status is one of:
--   NULL
--   'No Error'
--   'Run-Time Error'
--   'Time-Limit Exceeded'
--   'Compilation Error'
--   possibly others...
--
CREATE TABLE test_job (
	id               INTEGER AUTO_INCREMENT PRIMARY KEY,
	type             CHAR(1) NOT NULL,
	user_uid         VARCHAR(255),
	source_file      VARCHAR(200) NOT NULL,
	source_name      VARCHAR(200) NOT NULL,
	input_file       VARCHAR(200) NOT NULL,
	result_status    VARCHAR(200),
	result_detail    TEXT,
	output_file      VARCHAR(200),
	owner            VARCHAR(200),
	created          DATETIME,
	last_touched     DATETIME,
	runtime_limit    INT NOT NULL DEFAULT 50,
	callback_data    VARCHAR(200)
	);

--FOREIGN KEY (contest,problem_number) REFERENCES problem (contest,problem_number)
--FOREIGN KEY (expected_file_job) REFERENCES test_job (id)
CREATE TABLE system_test (
	contest         INTEGER NOT NULL,
	problem_number  INTEGER NOT NULL,
	test_number     INTEGER NOT NULL,
	input_file      VARCHAR(200) NOT NULL,
	expected_file   VARCHAR(200),
	expected_file_job INTEGER,
	example_input   CHAR(1) NOT NULL DEFAULT 'N',
	autojudge       CHAR(1) NOT NULL DEFAULT 'N',
	PRIMARY KEY (contest, problem_number, input_file),
	UNIQUE INDEX (contest, problem_number, test_number)
	);

--FOREIGN KEY (submission)  REFERENCES submission (id),
--FOREIGN KEY (test_file)   REFERENCES system_test (input_file),
--FOREIGN KEY (job)         REFERENCES test_job (id),
CREATE TABLE test_result (
	submission     INTEGER NOT NULL,
	test_file      VARCHAR(200) NOT NULL,
	job            INTEGER NOT NULL,
	result_status  VARCHAR(200),
	PRIMARY KEY (submission, test_file)
	);

--FOREIGN KEY (team)           REFERENCES team (team_number),
--FOREIGN KEY (problem_number) REFERENCES problem (problem_number),
--FOREIGN KEY (judge)          REFERENCES judge (judge_id)
CREATE TABLE clarification (
	id             INTEGER AUTO_INCREMENT PRIMARY KEY,
	contest        INTEGER NOT NULL,
	problem_number INTEGER NOT NULL,
	team           INTEGER NOT NULL,
	created        DATETIME NOT NULL,
	request        TEXT NOT NULL,
	response       TEXT,
	status         VARCHAR(200),
	judge          INT
	);

--FOREIGN KEY (contest) REFERENCES contest (contest_id)
--recipient is one of:
--  '*' - everyone
--  'teams' - all teams
--  'judges' - all judges
--  'scoreboard' - all anonymous observers
--  'director' - the contest director
--  team username - a specific team
--  judge username - a specific judge
--messagetype is one of:
--  'N' - normal message; show in standard UI
--  'S' - scoreboard announcements; only display if on the scoreboard
CREATE TABLE announcement (
	id             INTEGER AUTO_INCREMENT PRIMARY KEY,
	contest        INTEGER NOT NULL,
	recipient      VARCHAR(255),
	message        VARCHAR(255),
	duration       INTEGER,
	messagetype    CHAR(1) NOT NULL DEFAULT 'N',
	url            VARCHAR(255),
	expires        DATETIME
	);
