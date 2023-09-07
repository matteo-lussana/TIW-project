
{
	var allcategories = null;
	var alloptions = null;

	var errorContainer = document.getElementById("errorMsg");
	var newCatButton = document.getElementById("newCatButton");
	var logoutButton = document.getElementById("logout");
	var saveZone = document.getElementById("saveZone");

	var tempAllCat;
	var tempAllCatOrdered = [];
	var newCatList = [];
	var ddCat;
	var subTreeToCopy = [];

	window.addEventListener("load", updateCategoryList(), false);
	
	function updateCategoryList(){
		if (sessionStorage.getItem("username") == null) {
			window.location.href = "LoginPage.html";
		}
		
		tempAllCat = [];
		tempAllCatOrdered = [];
		newCatList = [];
		ddCat = [];
		

		allcategories = new CategoryList(
			document.getElementById("categoryList"),
			document.getElementById("category")
		);
		allcategories.retrieveFromDB();


		alloptions = new Option(document.getElementById("fatherCodeList"));
		alloptions.showOpt();
	};

	function CategoryList(_categoryList, _categoryBody) {
		this.categoryList = _categoryList;
		this.categoryBody = _categoryBody;

		//makeCall to retrieve the categories from the server's database
		this.retrieveFromDB = function() {
			var self = this;
			makeCall("GET", "RetrieveAllCategories", null, function(req) {
				if (req.readyState === 4) {
					if (req.status === 200) {
						tempAllCat = JSON.parse(req.responseText);
						if (tempAllCat.length === 0) {
							errorContainer.textContent = "No categories yet";
							return;
						}
						subTreeToCopy.length = 0;
						self.manageTree();

					} 
					else if(req.status === 401){
						window.location.href = "LoginPage.html";
					}
					
					else {
							errorContainer.textContent = req.responseText;
					}
				}
			});
		};

		this.manageTree = function() {
			let self = this;
			self.printTree();
			self.manageDragAndDrop();
			self.manageNameEdit();
		}

		this.printTree = function() {
			let self = this;
			self.orderCategories();
			let codeCell, row, button, empty, ancestor = 0;
			this.categoryBody.innerHTML = "";
			for (let i = 0; i < tempAllCatOrdered.length; i++) {

				if (tempAllCatOrdered.at(i).code < 10) ancestor = tempAllCatOrdered.at(i).code;

				row = document.createElement("tr");
				codeCell = document.createElement("td");
				let lengthCode = parseInt(tempAllCatOrdered.at(i).code).toString().length;
				let space = " "
				for (let j = 0; j < lengthCode; j++) {
					space = space + "\xa0\xa0"
				}
				let code = tempAllCatOrdered.at(i).code;
				let name = tempAllCatOrdered.at(i).name;

				codeCell.textContent = space + code + " "+name;
				codeCell.setAttribute('id', code);
				
				//nameCell.textContent = space + name;
				//nameCell.setAttribute('id', code);
				
				row.appendChild(codeCell);
				//row.appendChild(nameCell);
				
				if(subTreeToCopy.length > 0 && self.isInFamily(code, name))
					row.setAttribute('class', "inFamily");
				else
					row.setAttribute('class', "draggable");
					
				row.setAttribute('draggable', true);
				row.setAttribute('id', code);
				self.categoryBody.appendChild(row);

			}
			if (ancestor < 9) {
				row = document.createElement("tr");
				codeCell = document.createElement("td");
				codeCell.textContent = "---";
				codeCell.setAttribute('id', 0);
				row.appendChild(codeCell);
				row.setAttribute('class', "spareRow");
				row.setAttribute('draggable', true);
				row.setAttribute('id', 0);
				row.setAttribute('name', 0);
				self.categoryBody.appendChild(row);
			}

			//self.manageDragAndDrop();

		};
		
		this.isInFamily = function(code, name){
			
			let flag = false;
			
			subTreeToCopy.forEach(c => {
				if(c.code == code && c.name == name)
					flag = true;
			});
			
			return flag;
		};

		this.manageDragAndDrop = function() {
			let self = this;
			ddCat = document.querySelectorAll(".draggable"); //all the draggable categories
			let familyFather;
			ddCat.forEach(c => {
				c.addEventListener("dragstart", dragStart);
				if (self.isDroppable(c.id)) {
					c.addEventListener("dragover", dragOver);
					c.addEventListener("drop", drop);
				}
			});
			
			let spareRow = document.querySelectorAll(".spareRow");
			if(spareRow != null){
				spareRow.forEach( el => {
					el.addEventListener("dragstart", (e) => {
							e.preventDefault();
					});
					el.addEventListener("dragover", dragOver);
					el.addEventListener("drop", drop);
				})
			}			
			
			function dragStart(e){
				familyFather = e.target.id;
			}
			function dragOver(e){
				e.preventDefault();
			}
			function drop(e){
				let target = e.target.id;
						if (self.copyIsPossible(familyFather, target)) {
							let confirm;
							if(target == 0)
								confirm = window.confirm("do you want to make " + familyFather + " a new ancestor, followed by its sub-tree?");
							else
								confirm = window.confirm("do you want to copy " + familyFather + " and its sub-tree in " + target + "?");
							if (confirm){
								self.copyFamily(familyFather, target);
							}
								
						}
						else if (target != familyFather)
							window.alert("ERROR: you cannot copy here the sub-tree because it's too deep");
			}

		};

		this.manageNameEdit = function() {
			let self = this;
			let rows = document.querySelectorAll(".draggable");

			rows.forEach(c => {
				c.addEventListener("click", (e) => {
					var input = document.createElement('input');
					input.type = 'text';
					
					let info = c.lastChild.textContent;
					let id = c.lastChild.id;
					let codeCell = document.createElement('label');
					let lengthCode = parseInt(id).toString().length;
					let d = document.createElement('div');
					d.setAttribute('class', "inLine");
					
					let space = " "
					for (let j = 0; j < lengthCode; j++) {
						space = space + "\xa0\xa0"
					}
					
					codeCell.textContent = space + id;
					d.appendChild(codeCell);
					d.appendChild(input);
					
					c.removeChild(c.lastChild);
					c.appendChild(d);
					input.focus();
					var code = e.target.id;
					input.addEventListener("blur", (e) => {
						var newName = input.value;
						if (newName != null && newName != "" && !newName.startsWith(" ") && code != null && code != "") {
							let categoryChanged = {
								"code": code,
								"name": newName
							};

							let categoryChangedJSON = JSON.stringify(categoryChanged);

							makeCallJSON("POST", 'ChangeName', categoryChangedJSON, function(req) {
								if (req.readyState == XMLHttpRequest.DONE) {
									var message = req.responseText;
									if (req.status === 200) {
										//c.removeChild(inputCell);
										updateCategoryList();
									}
									else {
										document.getElementById("saveError").textContent = message;
									}
								}
							});
						}
						else {
							c.removeChild(c.lastChild);
							let newCell = document.createElement('td');
							newCell.textContent = info;
							newCell.setAttribute('id', id);
							c.appendChild(newCell);
						}
					})
				})
			});
		};

		this.isDroppable = function(target) {
			let self = this;
			let targetPos = self.research(target);
			return !tempAllCatOrdered.at(targetPos).isComplete;
		};

		this.copyIsPossible = function(father, target) {
			
			if (target == 0) return true;

			if (father == target) return false;

			//check if the target category is complete
			let self = this;
			let targetPos = self.research(target);
			if (tempAllCatOrdered.at(targetPos).isComplete == true) return false;

			//check if the depth of the target category + the depth of the family to copy is less then 15
			let posFather = self.research(father);
			let family = [];
			family.push(tempAllCatOrdered.at(posFather));

			let longestCode = father;
			for (let i = posFather + 1; i <= tempAllCatOrdered.length - 1 && tempAllCatOrdered.at(i).code > tempAllCatOrdered.at(posFather).code * 10; i++) {
				family.push(tempAllCatOrdered.at(i));
			}
			family.forEach(c => {
				if (longestCode < c.code) longestCode = c.code;
			});

			let descendantsNum = longestCode.toString().length - father.toString().length;

			if (descendantsNum + target.toString().length >= 15) return false;
			return true;
		}

		this.copyFamily = function(father, target) {
			let self = this;
			
			//create the newCatList containing the family to copy
			let posFather = self.research(father);
			newCatList.push(tempAllCatOrdered.at(posFather));
			for (let i = posFather + 1; i < tempAllCatOrdered.length && tempAllCatOrdered.at(i).code > tempAllCatOrdered.at(posFather).code * 10; i++) {
				newCatList.push(tempAllCatOrdered.at(i));
			}
			
			let newFatherCode;
			
			if(target == 0){
				let lastAncestor = tempAllCatOrdered.at(tempAllCatOrdered.length-1).code.toString().at(0);
				lastAncestor = parseInt(lastAncestor);
				newFatherCode = lastAncestor + 1;
			}
			else
				//compute the correct codes
				newFatherCode = self.findLastChild(target) + 1; //se è -1???
			
			self.moveFamily(newFatherCode);
			self.manageTree();

			//block the drag and drop
			ddCat.forEach(c => {
				c.setAttribute('draggable', false);
			});
			
			//save or cancel?
			let div = document.createElement("div");
			let p = document.createElement("p");
			let saveB = document.createElement("input");
			let cancelB = document.createElement("input");
			
			p.textContent = "Do you want to save the changes made?";
			saveB.setAttribute('type', "button");
			saveB.setAttribute('value', "SAVE");
			saveB.setAttribute('class', "saveButton");
			cancelB.setAttribute('type', "button");
			cancelB.setAttribute('value', "CANCEL");
			cancelB.setAttribute('class', "saveButton");
			
			div.setAttribute('class', "inLineAndCentered");
			div.appendChild(saveB);
			div.appendChild(cancelB);
			
			saveZone.appendChild(p);
			saveZone.appendChild(div);

			saveB.addEventListener("click", (e) => {

				let movingFamily = {
					"familyFather": father,
					"targetCode": target
				}

				let movingFamilyJSON = JSON.stringify(movingFamily);
				makeCallJSON("POST", 'InsertFamily', movingFamilyJSON, function(req) {
					if (req.readyState == XMLHttpRequest.DONE) {
						var message = req.responseText;
						if (req.status === 200) {
							while(saveZone.firstChild){
								saveZone.removeChild(saveZone.firstChild)
							}
							subTreeToCopy.length = 0;
							updateCategoryList();
						}
						else {
							document.getElementById("saveError").textContent = message;
						}
					}
				});
			});

			cancelB.addEventListener("click", (e) => {
				newCatList.forEach(c => {
					if (tempAllCat.includes(c))
						tempAllCat.pop(c);
				});
				newCatList.length = 0;
				subTreeToCopy.length = 0;
				while(saveZone.firstChild){
					saveZone.removeChild(saveZone.firstChild)
				}
				
				self.manageTree();

			});

		};

		this.binaryResearch = function(toFind) {
			let end = tempAllCat.length - 1;
			let start = 0;
			let mid;

			while (start <= end) {
				mid = (start + end) / 2;
				mid = parseInt(mid);
				if (tempAllCat.at(mid).code == toFind) return mid;
				else if (tempAllCat.at(mid).code < toFind) start = mid + 1;
				else end = mid - 1;
			}
			return -1;
		};

		this.moveFamily = function(newFatherCode) {
			let self = this;
			let digitCountFather = newCatList.at(0).code.toString().length;

			//add the father with the correct code to the tempAllCat
			let newCat = {
				"code": newFatherCode,
				"father": parseInt(newFatherCode / 10),
				"name": newCatList.at(0).name
			};
			tempAllCat.push(newCat);
			subTreeToCopy.push(newCat);

			//set the new codes also in the newCatList in case the user clicks "CANCEL"
			//newCatList.at(0).code = newFatherCode;
			for (let i = 1; i < newCatList.length; i++) {
				let digitsNum = newCatList.at(i).code.toString().length;
				let staticDigits = parseInt(newCatList.at(i).code % (Math.pow(10, digitsNum - digitCountFather)));
				let newCode = staticDigits + parseInt(Math.pow(10, digitsNum - digitCountFather) * newFatherCode);

				newCat = {
					"code": newCode,
					"father": parseInt(newCode / 10),
					"name": newCatList.at(i).name
				};
				tempAllCat.push(newCat);
				subTreeToCopy.push(newCat);
			}
		};

		//not binary because the array is ordered with the codes
		this.research = function(toFind) {
			for (let i = 0; i < tempAllCatOrdered.length; i++) {
				if (tempAllCatOrdered.at(i).code == toFind)
					return i;
			}
			return -1;
		};

		this.findLastChild = function(code) {
			let self = this;
			let lastChildCode, pos, categoriesList;
			
			if (code == 0) {
				categoriesList = tempAllCat;
				pos = 0;
			}
			else {
				categoriesList = tempAllCatOrdered;
				pos = self.research(code);
			}
			
						
			if (pos+1 != categoriesList.length && categoriesList.at(pos + 1).code == code * 10 + 1) {
				let i = 1;
				while (true) {
					if (i === 10) {
						lastChildCode = -1;
						break;
					}
					else if (self.binaryResearch(code * 10 + i) != -1) {
						lastChildCode = code * 10 + i;
						i++;
					}
					else break;
				}
				if (lastChildCode === -1) {
					newCatList.removeAll(); //finire
				}
			}
			else {
				lastChildCode = code * 10;
			}
			return lastChildCode;
		};

		this.orderCategories = function() {
			let self = this;
			tempAllCatOrdered.length = 0;
			tempAllCat.forEach(function(category) {
				if (category.father === 0) {
					tempAllCatOrdered.push(category);
					self.fragment(category.code, tempAllCat, tempAllCatOrdered);
				}

			});
		};

		this.fragment = function(fatherCode, categoriesList, categoriesInOrder) {
			var self = this;
			categoriesList.forEach(function(category) {
				if (category.father === fatherCode) {
					categoriesInOrder.push(category);
					self.fragment(category.code, categoriesList, categoriesInOrder);
				}

			});
		}
	}

	function Option(_fatherCodeList) {
		this.fatherCodeList = _fatherCodeList;
		this.showOpt = function() {
			let self = this;
			makeCall("GET", "RetrieveAllCategories", null, function(req) {
				if (req.readyState === 4) {
					if (req.status === 200) {
						let optionCategories = JSON.parse(req.responseText);
						if (optionCategories.length === 0)
							return;
						self.updateOpt(optionCategories);
					} else {
						errorContainer.textContent = req.responseText;
					}
				}
			});
		};

		this.updateOpt = function(optionCategories) {
			let self = this;
			this.fatherCodeList.innerHTML = "";
			optionCategories.forEach(function(category) {
				if (!category.isComplete) {
					let optValue = document.createElement("option");
					optValue.textContent = category.code + " " + category.name;
					optValue.value = category.code;
					self.fatherCodeList.appendChild(optValue);
				}
			});
			
			//count the ancestors and set invisible the checkbox if the count is nine
			let checkbox = document.querySelectorAll("#isCheckboxFather");
			let ancestorsNum = 0;
			tempAllCat.forEach(c => {
				if(c.father == 0)
					ancestorsNum ++;
			});
			
			if(ancestorsNum == 9)
				checkbox.forEach(function(el){
					el.setAttribute('class', "invisible");
				});
		}
	}

	newCatButton.addEventListener("click", (e) => {
		if(newCatList.length == 0){
			let self = this;
			var form = e.target.closest("form");
			if (form.checkValidity()) {
				makeCall("POST", 'CreateCategory', e.target.closest("form"), function(req) {
					if (req.readyState == XMLHttpRequest.DONE) {
						var message = req.responseText;
						if (req.status === 200) {
							document.getElementById("formErrorMsg").textContent = "";
							updateCategoryList();
						}
						else {
							document.getElementById("formErrorMsg").textContent = message;
						}
					}
				});
			}
			else {
				form.reportValidity();
			}
		}
		else{
			document.getElementById("formErrorMsg").textContent = "You cannot create a new category while inserting a sub-tree";
		}
	});

	logout.addEventListener("click", (e) => {
		var form = e.target.closest("form");
		if (form.checkValidity()) {
			makeCall("GET", 'Logout', e.target.closest("form"), function(req) {
				if (req.readyState == XMLHttpRequest.DONE) {
					var message = req.responseText;
					if (req.status === 200) {
						sessionStorage.setItem("username", null);
						window.location.href = "LoginPage.html";
					}
					else {
						errorContainer.textContent = message;
					}
				}
			});
		}
		else {
			form.reportValidity();
		}
	});

};