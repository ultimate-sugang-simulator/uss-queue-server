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
import uuid


class SSELoadTester:
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
        url = f"{self.base_url}/api/v1/queue/sub?studentId={student_id}"

        try:
            async with session.get(url, timeout=aiohttp.ClientTimeout(total=None)) as response:
                if response.status != 200:
                    self.stats['failed'] += 1
                    print(f"[FAIL] {student_id} | HTTP {response.status}")
                    return

                self.stats['connected'] += 1
                print(f"[CONN] {student_id} | Connected (total: {self.stats['connected']})")

                # SSE 메시지 수신
                message_count = 0
                async for line in response.content:
                    if not line:
                        continue

                    message = line.decode('utf-8').strip()
                    if not message.startswith('data:'):
                        continue

                    self.stats['messages_received'] += 1
                    message_count += 1

                    # JSON 파싱
                    try:
                        json_data = json.loads(message[5:])  # 'data:' 제거
                        is_accessible = json_data.get('isAccessible', False)
                        waiting_count = json_data.get('waitingCount')
                        token = json_data.get('token')

                        if is_accessible and token:
                            self.stats['admitted'] += 1
                            print(f"[ADMIT] {student_id} | Admitted | token: {token[:10]}... | waited: {message_count} msgs")
                            break
                        elif waiting_count is not None and message_count % 5 == 1:
                            # 5개 메시지마다 한 번씩만 출력
                            print(f"[WAIT] {student_id} | Queue position: {waiting_count}")

                    except json.JSONDecodeError:
                        if message_count == 1:
                            print(f"[WARN] {student_id} | Parse failed: {message[:50]}")

        except asyncio.TimeoutError:
            self.stats['failed'] += 1
            print(f"[TIMEOUT] {student_id}")
        except Exception as e:
            self.stats['failed'] += 1
            error_msg = f"{student_id} | {type(e).__name__}: {str(e)}"
            print(f"[ERROR] {error_msg}")
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
                self.connect_sse(session, f"user_{str(uuid.uuid4())[:6]}")
                for i in range(self.num_connections)
            ]

            try:
                await asyncio.wait_for(
                    asyncio.gather(*tasks, return_exceptions=True),
                    timeout=self.duration
                )
            except asyncio.TimeoutError:
                print(f"\n[TIMEOUT] Test duration limit reached ({self.duration}s)")

        self._print_results()

    def _print_header(self):
        print(f"\n{'='*80}")
        print(f"SSE Queue Load Test")
        print(f"{'='*80}")
        print(f"Server URL       : {self.base_url}")
        print(f"Connections      : {self.num_connections}")
        print(f"Max Duration     : {self.duration}s")
        print(f"Start Time       : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'='*80}\n")

    def _print_results(self):
        elapsed = time.time() - self.start_time
        success_rate = (self.stats['connected'] / self.num_connections * 100) if self.num_connections > 0 else 0
        admitted_rate = (self.stats['admitted'] / self.stats['connected'] * 100) if self.stats['connected'] > 0 else 0
        msg_per_sec = self.stats['messages_received'] / elapsed if elapsed > 0 else 0

        print(f"\n{'='*80}")
        print(f"Test Results")
        print(f"{'='*80}")
        print(f"Total Attempts   : {self.num_connections}")
        print(f"Connected        : {self.stats['connected']} ({success_rate:.1f}%)")
        print(f"Failed           : {self.stats['failed']}")
        print(f"Admitted         : {self.stats['admitted']} ({admitted_rate:.1f}% of connected)")
        print(f"Messages         : {self.stats['messages_received']} ({msg_per_sec:.1f} msg/s)")
        print(f"Duration         : {elapsed:.2f}s")
        print(f"{'='*80}")

        # 평가
        self._print_evaluation(success_rate, admitted_rate)

        # 에러 상세
        if self.stats['errors']:
            print(f"\nErrors (showing first 10):")
            for error in self.stats['errors'][:10]:
                print(f"  {error}")
            if len(self.stats['errors']) > 10:
                print(f"  ... and {len(self.stats['errors']) - 10} more")
            print()

    def _print_evaluation(self, success_rate, admitted_rate):
        print()
        if success_rate >= 90:
            print("Status: PASS | 90%+ connections succeeded")
        elif success_rate >= 70:
            print("Status: WARN | 70-90% connections (optimization recommended)")
        else:
            print("Status: FAIL | <70% connections (system capacity exceeded)")

        if self.stats['connected'] > 0:
            if admitted_rate >= 80:
                print("Queue:  GOOD | 80%+ users admitted")
            elif admitted_rate >= 50:
                print("Queue:  SLOW | 50-80% users admitted (check scheduler)")
            else:
                print("Queue:  POOR | <50% users admitted (scheduler issue?)")


def check_server_connection(url):
    print("Checking server connection...\n")
    try:
        import requests
        response = requests.get(
            f"{url}/api/v1/queue/sub?studentId=health_check",
            timeout=1,
            stream=True
        )
        print(f"[OK] Server responding (HTTP {response.status_code})\n")
        return True
    except Exception as e:
        print(f"[FAIL] Cannot connect to server: {e}")
        print("\nPlease ensure server is running:")
        print("  $ ./gradlew bootRun\n")
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
        print("\n\n[INTERRUPTED] Test stopped by user (Ctrl+C)")
        if tester.start_time:
            tester._print_results()
        sys.exit(130)


if __name__ == "__main__":
    main()
