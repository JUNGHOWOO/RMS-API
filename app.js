// ------- * 프로젝트에서 라우팅을 담당하는 app.js 코드

/* ------- * 코드 구조
1. 라이브러리 선언
2. 리우터 선언
3. arguments 값을 입력
4. express 함수 선언
5. 에러 핸들러 404
6. 에러 핸들러
7. 포트 설정
99. 라우터 모듈 선언
*/

// 1. 라이브러리 선언
var createError = require('http-errors');
var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var app = express();

// 2. 리우터 선언
var rmsRouter = require('./routes/v1/rms'); // 최초 코드
var rmsRouterv2 = require('./routes/v2/rms'); // 보완 코드

// 3. 서버 시작시에 arguments 값을 입력 받아서 포트와 베이스폴더 경로를 dev/oper로 구분
const args = process.argv.slice(2);

let argMap = {};

if (args.length === 0){
  console.log('required arguments');
  process.exit(1);
}

if (args.length % 2){
  console.log('Invalid number of arguments');
  process.exit(1);
}

for (let i = 0; i < args.length; i += 2) {
	let key = args[i];
	
	if (!/^--([a-z]+-)*[a-z]+$/g.test(key))
		throw new Error('Invalid argument name');
	
	key = key
		.replace(/^--/, '')
		.replace(/-([a-z])/g, g => g[1].toUpperCase());
	
    argMap[key] = args[i + 1];
}

let activeConfigFileName = "./config-" + argMap.profile;

global.config = require(activeConfigFileName);


// 4. express 함수 선언
app.use(logger('dev'));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json({limit : "10mb"}));
app.use(express.urlencoded({limit:"10mb", extended:true}));
app.use('/rms', rmsRouter); //기존 코드
app.use('/v2/rms', rmsRouterv2); //트랜잭션 및 암복호화 적용 코드


// 5. 에러 핸들러 404
app.use(function(req, res, next) {

  next(createError(404));

});


// 6. 에러 핸들러
app.use(function(err, req, res, next) {

  res.locals.message = err.message;

  console.log("error : " + err.message);

  res.locals.error = req.app.get('env') === 'development' ? err : {};

  res.json({responseCode:"2000", msg:"request entity too large"});

});


// 7. 포트 설정
const port = process.env.PORT || 3000;

app.listen(port, () => console.log(`Server is running on port ${port}`));


// 99. 모듈 선언
module.exports = app;