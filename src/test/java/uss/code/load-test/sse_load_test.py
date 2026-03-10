#!/usr/bin/env python3

# 사용 예시:
# 기본 테스트 (100명, 120초)
# python3 sse_load_test.py

# 소규모 테스트 (50명, 30초)
# python3 sse_load_test.py --connections 50 --duration 30

# 중규모 테스트 (500명, 2분)
# python3 sse_load_test.py --connections 500 --duration 120

# 대규모 테스트 (1000명, 3분)
# python3 sse_load_test.py --connections 1000 --duration 180

import asyncio
import aiohttp
import json
import time
import argparse
from datetime import datetime
import sys


class SSELoadTester:
    """SSE 대기열 서버 부하 테스트 클래스"""

    def __init__(self, base_url, num_connections, duration):
        self.base_url = base_url
        self.num_connections = num_connections
        self.duration = duration
        self.stats = {
            'connected': 0,
            'failed': 0,
            'admitted': 0,  # 입장 처리된 사용자
            'messages_received': 0,
            'errors': []
        }
        self.start_time = None

    async def connect_sse(self, session, student_id):
        """SSE 연결 수립 및 메시지 수신"""
        url = f"{self.base_url}/api/v1/queue/sub?studentId={student_id}"

        try:
            async with session.get(url, timeout=aiohttp.ClientTimeout(total=None)) as response:
                if response.status != 200:
                    self.stats['failed'] += 1
                    print(f"✗ [{student_id}] 연결 실패: HTTP {response.status}")
                    return

                self.stats['connected'] += 1
                print(f"✓ [{student_id}] 연결 성공 (총 {self.stats['connected']}개)")

                # SSE 메시지 수신
                async for line in response.content:
                    if not line:
                        continue

                    message = line.decode('utf-8').strip()
                    if not message.startswith('data:'):
                        continue

                    self.stats['messages_received'] += 1

                    # JSON 파싱
                    try:
                        json_data = json.loads(message[5:])  # 'data:' 제거
                        is_accessible = json_data.get('isAccessible', False)
                        waiting_count = json_data.get('waitingCount')
                        token = json_data.get('token')

                        if is_accessible and token:
                            self.stats['admitted'] += 1
                            print(f"✓ [{student_id}] 입장 승인 (token={token[:8]}...) - 연결 종료")
                            break
                        elif waiting_count is not None:
                            print(f"  [{student_id}] 대기 중 (앞에 {waiting_count}명)")
                        else:
                            print(f"  [{student_id}] 연결됨")

                    except json.JSONDecodeError:
                        print(f"  [{student_id}] 알 수 없는 메시지: {message[:60]}...")

        except asyncio.TimeoutError:
            self.stats['failed'] += 1
            print(f"✗ [{student_id}] 타임아웃")
        except Exception as e:
            self.stats['failed'] += 1
            error_msg = f"[{student_id}] {type(e).__name__}: {str(e)}"
            print(f"✗ {error_msg}")
            self.stats['errors'].append(error_msg)

    async def run_test(self):
        """부하 테스트 실행"""
        self._print_header()
        self.start_time = time.time()

        # ClientSession 설정
        connector = aiohttp.TCPConnector(limit=self.num_connections + 100)
        timeout = aiohttp.ClientTimeout(total=None, connect=10)

        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            tasks = [
                self.connect_sse(session, f"student_{i}")
                for i in range(self.num_connections)
            ]

            try:
                await asyncio.wait_for(
                    asyncio.gather(*tasks, return_exceptions=True),
                    timeout=self.duration
                )
            except asyncio.TimeoutError:
                print(f"\n⏱ 테스트 시간 {self.duration}초 경과")

        self._print_results()

    def _print_header(self):
        """테스트 헤더 출력"""
        print(f"\n{'='*70}")
        print(f"SSE 대기열 서버 부하 테스트")
        print(f"{'='*70}")
        print(f"서버 URL          : {self.base_url}")
        print(f"동시 연결 수       : {self.num_connections}명")
        print(f"최대 테스트 시간   : {self.duration}초")
        print(f"시작 시간         : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'='*70}\n")

    def _print_results(self):
        """테스트 결과 출력"""
        elapsed = time.time() - self.start_time
        success_rate = (self.stats['connected'] / self.num_connections * 100) if self.num_connections > 0 else 0
        admitted_rate = (self.stats['admitted'] / self.stats['connected'] * 100) if self.stats['connected'] > 0 else 0

        print(f"\n{'='*70}")
        print(f"테스트 결과")
        print(f"{'='*70}")
        print(f"총 시도 연결 수    : {self.num_connections}명")
        print(f"성공한 연결 수     : {self.stats['connected']}명 ({success_rate:.1f}%)")
        print(f"실패한 연결 수     : {self.stats['failed']}명")
        print(f"입장 처리된 수     : {self.stats['admitted']}명 ({admitted_rate:.1f}%)")
        print(f"수신한 메시지 수   : {self.stats['messages_received']}개")
        print(f"소요 시간         : {elapsed:.2f}초")
        print(f"{'='*70}\n")

        # 평가
        self._print_evaluation(success_rate)

        # 에러 상세
        if self.stats['errors']:
            print(f"\n에러 상세 (최대 10개):")
            for error in self.stats['errors'][:10]:
                print(f"  - {error}")
            if len(self.stats['errors']) > 10:
                print(f"  ... 외 {len(self.stats['errors']) - 10}개")

    def _print_evaluation(self, success_rate):
        """테스트 평가 출력"""
        if success_rate >= 90:
            print("✅ 테스트 통과: 90% 이상 연결 성공")
        elif success_rate >= 70:
            print("⚠️  경고: 70~90% 연결 성공, 시스템 최적화 권장")
        else:
            print("❌ 테스트 실패: 70% 미만 연결 성공, 시스템 한계 도달")


def check_server_connection(url):
    """서버 연결 사전 테스트"""
    print("서버 연결 확인 중...\n")
    try:
        import requests
        response = requests.get(
            f"{url}/api/v1/queue/sub?studentId=health_check",
            timeout=5,
            stream=True
        )
        print(f"✓ 서버 응답 확인 (HTTP {response.status_code})")
        return True
    except Exception as e:
        print(f"✗ 서버 연결 실패: {e}")
        print("\n서버가 실행 중인지 확인하세요:")
        print("  $ ./gradlew bootRun")
        return False


def main():
    parser = argparse.ArgumentParser(
        description='SSE 대기열 서버 부하 테스트',
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        '--url',
        default='http://localhost:8080',
        help='서버 URL (기본: http://localhost:8080)'
    )
    parser.add_argument(
        '--connections',
        type=int,
        default=100,
        help='동시 연결 수 (기본: 100)'
    )
    parser.add_argument(
        '--duration',
        type=int,
        default=120,
        help='최대 테스트 시간(초) (기본: 120)'
    )

    args = parser.parse_args()

    # 서버 연결 확인
    if not check_server_connection(args.url):
        sys.exit(1)

    # 부하 테스트 실행
    tester = SSELoadTester(args.url, args.connections, args.duration)

    try:
        asyncio.run(tester.run_test())
    except KeyboardInterrupt:
        print("\n\n⚠️  테스트 중단됨 (Ctrl+C)")
        if tester.start_time:
            tester._print_results()
        sys.exit(130)


if __name__ == "__main__":
    main()
