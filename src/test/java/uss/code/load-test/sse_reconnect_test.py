#!/usr/bin/env python3

# SSE 대기열 서버 스케줄러 Cleanup 테스트
#
# 테스트 목적:
# 1. 연결이 끊겼을 때 4초 스케줄러가 제대로 cleanup하는지 확인
# 2. cleanup 후 같은 사용자가 다시 sub할 때 정상적으로 대기열에 등록되는지 확인
#
# 사용 예시:
#
# 기본 테스트 (30명, 60초 동안, 2초 연결 후 5초 대기)
# python3 sse_reconnect_test.py
#
# 짧은 연결 유지 (1초 연결 후 5초 대기)
# python3 sse_reconnect_test.py --connections 20 --hold-time 1 --wait-after-disconnect 5
#
# 극단적 테스트 (0.5초 연결 후 6초 대기)
# python3 sse_reconnect_test.py --connections 30 --hold-time 0.5 --wait-after-disconnect 6
#
# 빠른 테스트 (10명, 30초)
# python3 sse_reconnect_test.py --connections 10 --duration 30

import asyncio
import aiohttp
import json
import time
import argparse
from datetime import datetime
import sys
import uuid


class SSEReconnectTester:
    def __init__(self, base_url, num_connections, duration, hold_time, wait_after_disconnect, max_reconnects):
        self.base_url = base_url
        self.num_connections = num_connections
        self.duration = duration
        self.hold_time = hold_time  # 연결 유지 시간 (스케줄러 주기보다 짧게)
        self.wait_after_disconnect = wait_after_disconnect  # 연결 해제 후 대기 시간 (스케줄러가 cleanup할 시간)
        self.max_reconnects = max_reconnects  # 최대 재연결 횟수
        self.stats = {
            'total_connections': 0,  # 총 연결 시도 횟수
            'successful_connections': 0,  # 성공한 연결
            'failed_connections': 0,  # 실패한 연결
            'disconnections': 0,  # 의도적 연결 해제 횟수
            'admitted': 0,  # 입장 처리된 사용자
            'messages_received': 0,  # 받은 메시지 총 개수
            'reconnect_cycles': {},  # 각 사용자의 재연결 횟수
            'successful_reconnects': 0,  # cleanup 후 재연결 성공 횟수
            'errors': []
        }
        self.start_time = None
        self.active_tasks = set()

    async def connect_and_disconnect(self, session, student_id, reconnect_count):
        """연결 맺고 짧은 시간 후 연결 끊기 (스케줄러 주기보다 짧게)"""
        url = f"{self.base_url}/api/v1/queue/sub?studentId={student_id}"
        connect_time = time.time()

        try:
            self.stats['total_connections'] += 1

            async with session.get(url, timeout=aiohttp.ClientTimeout(total=30)) as response:
                if response.status != 200:
                    self.stats['failed_connections'] += 1
                    print(f"[FAIL] {student_id} (#{reconnect_count}) | HTTP {response.status}")
                    return False

                self.stats['successful_connections'] += 1
                if reconnect_count > 1:
                    self.stats['successful_reconnects'] += 1
                    print(f"[RECONN] {student_id} (#{reconnect_count}) | Reconnected successfully after cleanup")
                else:
                    print(f"[CONN] {student_id} (#{reconnect_count}) | Initial connection")

                # 메시지 수신 시작
                message_count = 0
                is_admitted = False

                # 지정된 시간만큼만 연결 유지
                try:
                    async with asyncio.timeout(self.hold_time):
                        async for line in response.content:
                            if not line:
                                continue

                            message = line.decode('utf-8').strip()
                            if not message.startswith('data:'):
                                continue

                            self.stats['messages_received'] += 1
                            message_count += 1

                            try:
                                json_data = json.loads(message[5:])
                                is_accessible = json_data.get('isAccessible', False)
                                waiting_count = json_data.get('waitingCount')
                                token = json_data.get('token')

                                if is_accessible and token:
                                    self.stats['admitted'] += 1
                                    is_admitted = True
                                    print(f"[ADMIT] {student_id} (#{reconnect_count}) | Admitted | token: {token[:10]}...")
                                    break
                                elif waiting_count is not None:
                                    print(f"[MSG] {student_id} (#{reconnect_count}) | Queue: {waiting_count} (held: {time.time()-connect_time:.1f}s)")

                            except json.JSONDecodeError:
                                pass
                except asyncio.TimeoutError:
                    # 의도적인 타임아웃 (연결 유지 시간 초과)
                    pass

                self.stats['disconnections'] += 1
                held_duration = time.time() - connect_time
                print(f"[DISC] {student_id} (#{reconnect_count}) | Disconnected (held: {held_duration:.1f}s, msgs: {message_count})")

                return is_admitted

        except asyncio.TimeoutError:
            self.stats['failed_connections'] += 1
            print(f"[TIMEOUT] {student_id} (#{reconnect_count})")
            return False
        except Exception as e:
            self.stats['failed_connections'] += 1
            error_msg = f"{student_id} (#{reconnect_count}) | {type(e).__name__}: {str(e)}"
            print(f"[ERROR] {error_msg}")
            self.stats['errors'].append(error_msg)
            return False

    async def reconnect_loop(self, session, student_id):
        """지정된 시간 동안 계속 재연결 시도 (스케줄러 cleanup 테스트)"""
        reconnect_count = 0
        end_time = self.start_time + self.duration

        while time.time() < end_time and reconnect_count < self.max_reconnects:
            reconnect_count += 1

            # 연결 시도
            is_admitted = await self.connect_and_disconnect(session, student_id, reconnect_count)

            # 입장되면 재연결 중단
            if is_admitted:
                print(f"[DONE] {student_id} | Admitted after {reconnect_count} attempts")
                break

            # 테스트 종료 시간 체크
            if time.time() >= end_time:
                break

            # 스케줄러가 cleanup할 시간 대기
            print(f"[CLEANUP] {student_id} | Waiting {self.wait_after_disconnect:.1f}s for scheduler cleanup (cycle every 4s)...")
            await asyncio.sleep(self.wait_after_disconnect)

        self.stats['reconnect_cycles'][student_id] = reconnect_count
        print(f"[END] {student_id} | Completed {reconnect_count} reconnection cycles")

    async def run_test(self):
        """재연결 테스트 실행"""
        self._print_header()
        self.start_time = time.time()

        # ClientSession 설정
        connector = aiohttp.TCPConnector(limit=self.num_connections * 2)
        timeout = aiohttp.ClientTimeout(total=None, connect=10)

        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            # 각 사용자별로 재연결 루프 태스크 생성
            tasks = [
                self.reconnect_loop(session, f"user_{str(uuid.uuid4())[:6]}")
                for i in range(self.num_connections)
            ]

            try:
                await asyncio.gather(*tasks, return_exceptions=True)
            except Exception as e:
                print(f"\n[ERROR] Test error: {e}")

        self._print_results()

    def _print_header(self):
        print(f"\n{'='*80}")
        print(f"SSE Queue Scheduler Cleanup Test")
        print(f"{'='*80}")
        print(f"Server URL                : {self.base_url}")
        print(f"Concurrent Users          : {self.num_connections}")
        print(f"Test Duration             : {self.duration}s")
        print(f"Connection Hold Time      : {self.hold_time}s (< 4s scheduler)")
        print(f"Wait After Disconnect     : {self.wait_after_disconnect}s (> 4s scheduler)")
        print(f"Max Reconnects/User       : {self.max_reconnects}")
        print(f"Scheduler Period          : 4s")
        print(f"Start Time                : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'='*80}")
        print(f"Test Strategy:")
        print(f"  1. Connect and hold for {self.hold_time}s (shorter than scheduler)")
        print(f"  2. Disconnect")
        print(f"  3. Wait {self.wait_after_disconnect}s for scheduler to cleanup")
        print(f"  4. Reconnect with same studentId")
        print(f"  5. Verify normal queue registration")
        print(f"{'='*80}\n")

    def _print_results(self):
        elapsed = time.time() - self.start_time
        total_cycles = sum(self.stats['reconnect_cycles'].values())
        avg_cycles = total_cycles / len(self.stats['reconnect_cycles']) if self.stats['reconnect_cycles'] else 0
        success_rate = (self.stats['successful_connections'] / self.stats['total_connections'] * 100) if self.stats['total_connections'] > 0 else 0
        reconnect_success_rate = (self.stats['successful_reconnects'] / (self.stats['total_connections'] - self.num_connections) * 100) if (self.stats['total_connections'] - self.num_connections) > 0 else 0
        conn_per_sec = self.stats['total_connections'] / elapsed if elapsed > 0 else 0

        print(f"\n{'='*80}")
        print(f"Test Results")
        print(f"{'='*80}")
        print(f"Duration               : {elapsed:.2f}s")
        print(f"Concurrent Users       : {self.num_connections}")
        print(f"Total Connections      : {self.stats['total_connections']} ({conn_per_sec:.2f} conn/s)")
        print(f"Successful             : {self.stats['successful_connections']} ({success_rate:.1f}%)")
        print(f"Failed                 : {self.stats['failed_connections']}")
        print(f"Successful Reconnects  : {self.stats['successful_reconnects']} ({reconnect_success_rate:.1f}%)")
        print(f"Disconnections         : {self.stats['disconnections']}")
        print(f"Messages Received      : {self.stats['messages_received']}")
        print(f"Users Admitted         : {self.stats['admitted']}")
        print(f"Total Reconnect Cycles : {total_cycles}")
        print(f"Avg Cycles/User        : {avg_cycles:.2f}")
        print(f"{'='*80}")

        # 평가
        self._print_evaluation(success_rate, reconnect_success_rate)

        # 재연결 통계 상세
        if self.stats['reconnect_cycles']:
            max_reconnects = max(self.stats['reconnect_cycles'].values())
            min_reconnects = min(self.stats['reconnect_cycles'].values())
            print(f"\nReconnection Stats:")
            print(f"  Min: {min_reconnects} | Max: {max_reconnects} | Avg: {avg_cycles:.2f}")
            print(f"  Successful Reconnects: {self.stats['successful_reconnects']}")

        # 에러 상세
        if self.stats['errors']:
            print(f"\nErrors (showing first 10):")
            for error in self.stats['errors'][:10]:
                print(f"  {error}")
            if len(self.stats['errors']) > 10:
                print(f"  ... and {len(self.stats['errors']) - 10} more")
        print()

    def _print_evaluation(self, success_rate, reconnect_success_rate):
        print()
        if success_rate >= 95:
            print("Overall Connection: EXCELLENT | 95%+ success rate")
        elif success_rate >= 85:
            print("Overall Connection: GOOD | 85-95% success rate")
        elif success_rate >= 70:
            print("Overall Connection: FAIR | 70-85% success rate")
        else:
            print("Overall Connection: POOR | <70% success rate (connection issue)")

        if self.stats['successful_reconnects'] > 0:
            if reconnect_success_rate >= 95:
                print("Scheduler Cleanup: EXCELLENT | 95%+ reconnects successful after cleanup")
            elif reconnect_success_rate >= 85:
                print("Scheduler Cleanup: GOOD | 85-95% reconnects successful")
            elif reconnect_success_rate >= 70:
                print("Scheduler Cleanup: FAIR | 70-85% reconnects successful (check cleanup logic)")
            else:
                print("Scheduler Cleanup: POOR | <70% reconnects successful (cleanup not working properly)")


def check_server_connection(url):
    print("Checking server connection...\n")
    try:
        import requests
        response = requests.get(
            f"{url}/api/v1/queue/sub?studentId=health_check",
            timeout=5,
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
        description='SSE 대기열 서버 스케줄러 cleanup 테스트 (연결 해제 후 재등록 검증)',
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
        default=30,
        help='동시 사용자 수 (기본: 30)'
    )
    parser.add_argument(
        '--duration',
        type=int,
        default=60,
        help='테스트 지속 시간(초) (기본: 60)'
    )
    parser.add_argument(
        '--hold-time',
        type=float,
        default=2.0,
        help='연결 유지 시간(초) - 스케줄러 주기보다 짧게 설정 (기본: 2.0)'
    )
    parser.add_argument(
        '--wait-after-disconnect',
        type=float,
        default=5.0,
        help='연결 해제 후 대기 시간(초) - 스케줄러가 cleanup할 시간 (기본: 5.0)'
    )
    parser.add_argument(
        '--max-reconnects',
        type=int,
        default=10,
        help='사용자당 최대 재연결 횟수 (기본: 10)'
    )

    args = parser.parse_args()

    # 파라미터 검증
    if args.hold_time >= 4.0:
        print("[WARN] hold-time should be < 4s (scheduler period) for proper testing")

    if args.wait_after_disconnect < 4.0:
        print("[WARN] wait-after-disconnect should be > 4s to allow scheduler cleanup")

    # 서버 연결 확인
    if not check_server_connection(args.url):
        sys.exit(1)

    # 재연결 테스트 실행
    tester = SSEReconnectTester(
        args.url,
        args.connections,
        args.duration,
        args.hold_time,
        args.wait_after_disconnect,
        args.max_reconnects
    )

    try:
        asyncio.run(tester.run_test())
    except KeyboardInterrupt:
        print("\n\n[INTERRUPTED] Test stopped by user (Ctrl+C)")
        if tester.start_time:
            tester._print_results()
        sys.exit(130)


if __name__ == "__main__":
    main()
