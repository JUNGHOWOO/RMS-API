// ------- * DB와의 연동과 CRUD 기능을 포함하는 쿼리문이 작성된 db.js

/* ------- * 코드 구조
0. MySql 선언 및 bjworld21_rms DB 연동
1. 요청 내역 및 첨부파일 DB 업데이트 로직
  └ 1.1 요청 내역의 DB 업데이트 로직
  └ 1.2 첨부파일 DB 업데이트 로직
2. APIKEY 값을 통한 DB 검색 로직
99. 모듈 선언
*/

// 0. MySql 선언 및 bjworld21_rms DB 연동 ===================================
const mysql = require('mysql');
const encString = "gi625@3vet1#"; // 암복호화 로직
const connection = mysql.createConnection(
    {
        host: 'www.bjworld21.com'
        , user: 'bjworld21_rms'
        , password: 'a2jdf@gj9#3d'
        , port: 3306
        , database: 'bjworld21_rms'
        , dateStrings: 'date'
    }
);

// 커넥션 에러 확인
connection.connect(function (error) {
    if (error) {
        console.error('mysql connection error');
        console.error(err);
        throw err;
    }
});

// 1. 요청 내역 및 첨부파일 DB 업데이트 로직 ==================================
// 1.1 요청 내역의 DB 업데이트 로직 -------------------------------------------
const regReq = async (req, callback) => {
        connection.query(`INSERT INTO request (company_api_key, reg_user_name, reg_user_phone, request_subject, request_contents) VALUES (       
                '${req.header('BJRMS-APIKEY')}'
                , HEX(AES_ENCRYPT('${req.body.regUserName}', SHA2('${encString}', 512)))
                , HEX(AES_ENCRYPT('${req.body.regUserPhone}', SHA2('${encString}', 512)))
                , '${req.body.requestSubject}'
                , '${req.body.requestContents}' 
                )`, (err, result, fields) => {
                    if (err) throw err;
                    callback(result);
                });   

}

// 1.2 첨부파일 DB 업데이트 로직 ----------------------------------------------
function regFile(req, res, requestSeq, files, callback) {
        connection.query(`INSERT INTO request_attachfile (request_seq, ori_filename, save_filename, filesize) VALUES (      
        '${requestSeq}'
        , '${files.oriFilename}'
        , '${files.saveFilename}'
        , '${files.filesize}' 
        )`,
        (err, result) => {
            if (err) throw err;
            callback(result);
        });
}

// 2. APIKEY 값을 통한 DB 검색 로직 =============================================
function apiSearch(req, callback) {
    connection.query(`SELECT * FROM request WHERE company_api_key='${req.header('BJRMS-APIKEY')}'`, (err, result, fields) => {
        if (err) throw err;
        callback(result);
    });
}

// 99. 모듈 선언 ================================================================
module.exports = {
    regReq
    , regFile
    , apiSearch
}