/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package config

import (
	"io/ioutil"
	"os"

	"gopkg.in/yaml.v2"
)

type RunConfig struct {
	Config Config `yaml:"config"`
}

type Config struct {
	Recursive bool `yaml:"recursive"`
	Namespace NamespaceConfig
}

type NamespaceConfig struct {
	Name 	   string `yaml:"name"`
	Temporary  bool   `yaml:"temporary"`
	AutoRemove bool   `yaml:"autoremove"`
}

func newWithDefaults() *RunConfig {
	ns := NamespaceConfig{
		AutoRemove: true,
		Temporary:  false,
	}

	var config = Config{Recursive: true, Namespace: ns}
	return &RunConfig{Config: config}
}

func LoadConfig(file string) (*RunConfig, error) {
	config := newWithDefaults()
	data, err := ioutil.ReadFile(file)
	if err != nil && os.IsNotExist(err) {
		return config, nil
	}
	if err = yaml.Unmarshal(data, config); err != nil {
		return nil, err
	}
	return config, nil
}
