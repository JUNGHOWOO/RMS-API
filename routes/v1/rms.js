// ------- * rms rest API 수신과 응답을 담당하며 컨트롤러 역할을 하는 rms.js

/* ------- * 코드 구조
0. 변수 및 라이브러리 선언
1. 요청 내역 및 첨부파일 DB 업데이트 로직
  └ 1.1 console 출력 및 DB 업로드 라우터
  └ 1.2 request DB 업데이트 정보 콘솔창 출력
  └ 1.3 request_attachfile DB 업데이트 정보 콘솔창 출력
  └ 1.4 첨부파일 유무에 따른 IF문 처리
  └ 1.5 uploads 폴더에 첨부파일 업로드 로직
  └ 1.6 앞서 선언한 완료 메세지를 await 함수를 이용하여 비동기적 출력
2. APIKEY 값을 통한 DB 검색 로직
99. 라우터 모듈 선언
*/

/* ------ * 보완점
1. 에러 처리
2. 동기 / 트랜잭션 처리 => 파일 DB에서 에러 발생 시, 요청 내역 DB 작업 롤백
3. 파일 데이터 암복호화
4. APIKEY 검색 기능 보완
 */

// 0. 변수 및 라이브러리 선언 ================================================
var express = require('express');
var router = express.Router();
const db = require('./db');
const fs = require('fs');
let resetFs = "\x1b[0m"; // 콘솔창 글씨 디자인 초기화

// 1. 요청 내역 및 첨부파일 DB 업데이트 로직 ==================================
// 1.1 console 출력 및 DB 업로드 라우터--------------------------------------
router.post('/createrequest2', function(req, res, next) { // 실사용을 위해서는 주소값의 변경이 필요
  // 1.2 request DB 업데이트 정보 콘솔창 출력------------------------------
  console.log('\u001b[45m', '------Message Start------', resetFs, "");
  db.regReq(req, async (result) => {
    // 1.3 request_attachfile DB 업데이트 정보 콘솔창 출력------------------
    console.log("\x1b[33m", '>> request DB: RequestInfo');
    console.log("\x1b[36m", "APIKEY : " + req.header('BJRMS-APIKEY'));
    console.log(resetFs, "UserName : " + req.body.regUserName);
    console.log(resetFs, "UserPhone : " + req.body.regUserPhone);
    console.log(resetFs, "Subject : " + req.body.requestSubject);
    console.log(resetFs, "Contents : " + req.body.requestContents);
    let requestSeq = 0;
    var files = req.body.fileArray;
    requestSeq = result.insertId;
    console.log(resetFs, "");
    console.log("\x1b[33m", ">> request_attachfile DB: NumOfFiles: " + files.length, resetFs);
    // 1.4 첨부파일 유무에 따른 IF문 처리
    if (files.length == 0) { // 첨부 파일이 없는 경우
      console.log("\x1b[31m", "DEBUG - feed: message: No attachments found", resetFs);
      responseData = { responseCode: "0000", responseMsg: "요청 내역을 저장하였습니다." };
    } else { // 첨부 파일이 있는 경우
      for (var i = 0; i < files.length; i++) {
        console.log("\x1b[36m", "requestSeq : " + requestSeq);
        console.log(resetFs, "oriFilename : " + files[i].oriFilename);
        console.log(resetFs, "fileData : " + files[i].fileData);
        console.log(resetFs, "savaFilename : " + files[i].saveFilename);
        console.log(resetFs, "filesize : " + files[i].filesize + "Byte");
        let uploadFile = files[i]; // regFile 내에서 쓰이는 변수
        db.regFile(req, res, requestSeq, uploadFile, function () {
          // 1.5 uploads 폴더에 첨부파일 업로드 로직 ------------------------
          let userUploadedImagePath = "C:\\Users\\이종하\\Desktop\\bjworld21_rms\\uploads\\" + uploadFile.saveFilename;
          fs.writeFile(userUploadedImagePath, uploadFile.fileData, function () {
            console.log(resetFs, "filepath : " + userUploadedImagePath);
            console.dir('DEBUG - feed: message: Saved to disk image attached by user');
            console.log('\u001b[41m', "-------Message end-------", resetFs);
            responseData = { responseCode: "0000", responseMsg: "요청 내역을 저장하였습니다." };
          });
        });

      }
    }
  });

});

// * 2. APIKEY 값을 통한 DB 검색 로직 =========================================
router.get('/getrequestcompanylist', function (req, res, next) {
  console.log('\u001b[45m', '------Search Start------', resetFs, "");
  console.log("APIKEY VALUE : " + req.header('BJRMS-APIKEY'));
  db.apiSearch(req, (result) => {
      res.json({isSuccess:"0000", msg:"검색에 성공하였습니다.", datas:result});
  });
});

// 99. 라우터 모듈 선언 =======================================================
module.exports = router;