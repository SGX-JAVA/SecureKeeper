/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Brenner, Colin Wulf
 */
#define htons(A) ((((int16_t)(A) & 0xff00) >> 8) | \
(((int16_t)(A) & 0x00ff) << 8))
#define htonl(A) ((((int32_t)(A) & 0xff000000) >> 24) | \
(((int32_t)(A) & 0x00ff0000) >> 8) | \
(((int32_t)(A) & 0x0000ff00) << 8) | \
(((int32_t)(A) & 0x000000ff) << 24))
#define ntohs htons
#define ntohl htonl
